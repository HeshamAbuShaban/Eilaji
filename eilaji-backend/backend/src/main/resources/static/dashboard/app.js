const API = location.origin + "/api/v1";
let token = null, role = null;
const $ = (id) => document.getElementById(id);
const log = (m) => { const el = $("log"); el.textContent = new Date().toLocaleTimeString() + "  " + m + "\n" + el.textContent; };
const auth = () => token ? { "Authorization": "Bearer " + token, "Content-Type": "application/json" } : { "Content-Type": "application/json" };

$("tabPharm").onclick = () => { $("panePharm").style.display = ""; $("paneCust").style.display = "none"; };
$("tabCust").onclick = () => { $("paneCust").style.display = ""; $("panePharm").style.display = "none"; };
$("btnRefresh").onclick = refreshAll;

$("btnLogin").onclick = async () => {
  const r = await fetch(API + "/auth/login", { method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: $("email").value.trim(), password: $("password").value }) });
  const j = await r.json();
  if (!j.success) { log("login failed: " + (j.error || r.status)); return; }
  token = j.data.accessToken || j.data.token;
  role = (j.data.user && j.data.user.role) || "?";
  $("who").textContent = (j.data.user ? j.data.user.email + " · " + role : "signed in");
  $("who").className = "pill ok";
  log("signed in as " + role);
  refreshAll();
};

const NEXT = { PENDING: "CONFIRMED", CONFIRMED: "PREPARING", PREPARING: "SHIPPED", SHIPPED: "DELIVERED" };

async function refreshAll() {
  if (!token) { log("sign in first"); return; }
  await Promise.all([loadOrders(), loadPrescs(), loadPharmacies(), loadMine()]);
}

let orderStatusById = {};
async function loadOrders() {
  const status = $("statusFilter").value;
  const r = await fetch(API + "/orders", { headers: auth() });
  const j = await r.json();
  const list = (j.data || []).filter(o => !status || o.status === status);
  orderStatusById = {};
  list.forEach(o => orderStatusById[o.id] = o.status);
  $("ordCount").textContent = list.length;
  $("orders").innerHTML = list.map(o => `
    <div class="order">
      <h4>#${o.id.slice(0, 8).toUpperCase()} · ${o.status}
        <span class="pill">${o.paymentStatus || ""}</span>
        ${o.handoffCode ? `<span class="pill ok">code ${o.handoffCode}</span>` : ""}</h4>
      <div class="meta">${o.pharmacyName || o.pharmacyId} · ${o.totalAmount} · ${o.deliveryAddress || ""}</div>
      <div class="actions">
        ${NEXT[o.status] ? `<button data-adv="${o.id}" data-to="${NEXT[o.status]}">→ ${NEXT[o.status]}</button>` : ""}
        ${o.status !== "DELIVERED" && o.status !== "CANCELLED" ? `<button class="ghost" data-adv="${o.id}" data-to="CANCELLED">Cancel</button>` : ""}
        ${o.paymentStatus === "PENDING" ? `<button class="ghost" data-pay="${o.id}">Mark COD collected</button>` : ""}
      </div>
    </div>`).join("") || "<p class='hint'>No orders.</p>";
  document.querySelectorAll("[data-adv]").forEach(b => b.onclick = () => advanceOrder(b.dataset.adv, b.dataset.to));
  document.querySelectorAll("[data-pay]").forEach(b => b.onclick = () => advanceOrder(b.dataset.pay, null, "COD_COLLECTED"));
}

async function advanceOrder(id, status, paymentStatus) {
  const body = {};
  if (status) body.status = status;
  if (paymentStatus) { body.status = orderStatusById[id] || "CONFIRMED"; body.paymentStatus = paymentStatus; }
  // status endpoint requires a status; when only collecting payment keep current status
  const r = await fetch(API + "/orders/" + id + "/status", { method: "PUT", headers: auth(), body: JSON.stringify(body) });
  const j = await r.json();
  log((status || paymentStatus) + " → " + id.slice(0, 8) + ": " + (j.success ? "ok" : (j.error || r.status)));
  loadOrders();
}
async function loadPrescs() {
  const r = await fetch(API + "/prescriptions?status=PENDING", { headers: auth() });
  const j = await r.json();
  const list = (j.data && j.data.items) || j.data || [];
  $("prescs").innerHTML = list.map(p => `
    <div class="order"><h4>Rx #${p.id.slice(0, 8)}</h4>
      <div class="meta">${p.notes || ""} · ${p.createdAt || ""}</div>
      <div class="actions">
        <input placeholder="quote" size="8" id="q-${p.id}" />
        <button data-quote="${p.id}">Quote &amp; accept</button>
      </div></div>`).join("") || "<p class='hint'>No pending prescriptions.</p>";
  document.querySelectorAll("[data-quote]").forEach(b => b.onclick = async () => {
    const id = b.dataset.quote, q = document.getElementById("q-" + id).value || "0";
    let r1 = await fetch(API + "/prescriptions/" + id + "/status", { method: "PUT", headers: auth(),
      body: JSON.stringify({ status: "RECEIVED_QUOTE", quotedPrice: parseFloat(q) }) });
    log("quote " + id.slice(0, 8) + ": " + r1.status);
    let r2 = await fetch(API + "/prescriptions/" + id + "/status", { method: "PUT", headers: auth(),
      body: JSON.stringify({ status: "ACCEPTED" }) });
    log("accept " + id.slice(0, 8) + ": " + r2.status);
    loadPrescs();
  });
}

async function loadPharmacies() {
  const sel = $("pharmSel");
  if (sel.options.length) return;
  const r = await fetch(API + "/pharmacies?page=0&pageSize=50", { headers: auth() });
  const j = await r.json();
  (j.data.items || []).forEach(p => {
    const o = document.createElement("option");
    o.value = p.id; o.textContent = p.name + " (" + p.city + ")";
    sel.appendChild(o);
  });
}

async function loadMine() {
  const r = await fetch(API + "/orders", { headers: auth() });
  const j = await r.json();
  const list = j.data || [];
  $("myOrders").innerHTML = list.slice(0, 10).map(o =>
    `<div class="order"><h4>#${o.id.slice(0, 8).toUpperCase()} · ${o.status}</h4>
     <div class="meta">${o.pharmacyName || ""} · ${o.totalAmount}</div></div>`).join("") || "<p class='hint'>No orders yet.</p>";
}

$("btnOrder").onclick = async () => {
  const body = { prescriptionId: null, pharmacyId: $("pharmSel").value,
    totalAmount: parseFloat($("custTotal").value || "0"),
    paymentMethod: "CASH", deliveryAddress: $("custAddr").value };
  const r = await fetch(API + "/orders", { method: "POST", headers: auth(), body: JSON.stringify(body) });
  const j = await r.json();
  log("place order: " + (j.success ? "ok " + j.data.id.slice(0, 8) : (j.error || r.status)));
  loadMine(); loadOrders();
};
