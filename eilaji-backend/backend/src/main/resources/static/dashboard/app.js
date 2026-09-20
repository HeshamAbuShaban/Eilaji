const API = location.origin + "/api/v1";
let token = localStorage.getItem("eilaji_dash_token") || null;
let role = localStorage.getItem("eilaji_dash_role") || null;
let myEmail = localStorage.getItem("eilaji_dash_email") || null;
const $ = (id) => document.getElementById(id);
const log = (m) => { const el = $("log"); el.textContent = new Date().toLocaleTimeString() + "  " + m + "\n" + el.textContent; };
const toast = (m, err) => {
  const t = document.createElement("div");
  t.className = "toast" + (err ? " err" : "");
  t.textContent = m;
  $("toasts").appendChild(t);
  setTimeout(() => t.remove(), 3200);
};
const auth = () => token ? { "Authorization": "Bearer " + token, "Content-Type": "application/json" } : { "Content-Type": "application/json" };
const esc = (s) => String(s == null ? "" : s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));

// theme
if (localStorage.getItem("eilaji_dash_theme") === "dark") { document.documentElement.dataset.theme = "dark"; $("darkToggle").checked = true; }
$("darkToggle").onchange = (e) => {
  document.documentElement.dataset.theme = e.target.checked ? "dark" : "";
  localStorage.setItem("eilaji_dash_theme", e.target.checked ? "dark" : "light");
};

// tabs
const panes = { tabPharm: "panePharm", tabStock: "paneStock", tabChats: "paneChats", tabCust: "paneCust" };
function showTab(id) {
  Object.entries(panes).forEach(([t, p]) => {
    $(p).style.display = t === id ? "" : "none";
    $(t).classList.toggle("active", t === id);
  });
}
Object.keys(panes).forEach(t => $(t).onclick = () => showTab(t));
$("btnRefresh").onclick = refreshAll;

// session
function paintSession() {
  if (token) {
    $("who").textContent = (myEmail || "?") + " · " + (role || "?");
    $("who").className = "pill ok";
    $("btnLogout").style.display = "";
    showTab(role === "PATIENT" ? "tabCust" : "tabPharm");
  } else {
    $("who").textContent = "signed out";
    $("who").className = "pill";
    $("btnLogout").style.display = "none";
  }
}
$("btnLogin").onclick = async () => {
  const r = await fetch(API + "/auth/login", { method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: $("email").value.trim(), password: $("password").value }) });
  const j = await r.json().catch(() => ({}));
  if (!j.success) { toast("Login failed: " + (j.error || r.status), true); log("login failed"); return; }
  token = j.data.accessToken || j.data.token;
  role = (j.data.user && j.data.user.role) || "?";
  myEmail = (j.data.user && j.data.user.email) || $("email").value.trim();
  localStorage.setItem("eilaji_dash_token", token);
  localStorage.setItem("eilaji_dash_role", role);
  localStorage.setItem("eilaji_dash_email", myEmail);
  paintSession();
  toast("Signed in as " + role);
  refreshAll();
};
$("btnLogout").onclick = () => {
  token = role = myEmail = null;
  ["eilaji_dash_token", "eilaji_dash_role", "eilaji_dash_email"].forEach(k => localStorage.removeItem(k));
  paintSession();
};

// live polling
let liveTimer = null;
$("liveToggle").onchange = (e) => {
  if (liveTimer) { clearInterval(liveTimer); liveTimer = null; }
  if (e.target.checked) { liveTimer = setInterval(() => { if (!document.hidden) refreshAll(true); }, 10000); toast("Live polling on"); }
};
document.addEventListener("visibilitychange", () => { /* polling loop self-pauses via hidden check */ });

const NEXT = { PENDING: "CONFIRMED", CONFIRMED: "PREPARING", PREPARING: "SHIPPED", SHIPPED: "DELIVERED" };
let orderStatusById = {};
let orderCache = [];
let pharmCache = [];

async function refreshAll(quiet) {
  if (!token) { if (!quiet) toast("Sign in first", true); return; }
  await Promise.all([loadOrders(quiet), loadPrescs(quiet), loadPharmacies(), loadMine(quiet), loadChats(quiet)]);
}
function skel(el, n) {
  el.innerHTML = Array.from({ length: n || 2 }, () => '<div class="skel"></div>').join("");
}

// ---- orders ----
async function loadOrders(quiet) {
  const status = $("statusFilter").value, q = $("orderSearch").value.trim().toLowerCase();
  if (!quiet) skel($("orders"), 2);
  let list = [];
  try {
    const r = await fetch(API + "/orders", { headers: auth() });
    const j = await r.json();
    list = j.data || [];
  } catch (e) { if (!quiet) toast("Orders failed: " + e.message, true); return; }
  orderCache = list;
  const view = list.filter(o => (!status || o.status === status) && (!q || o.id.toLowerCase().includes(q)));
  orderStatusById = {};
  view.forEach(o => orderStatusById[o.id] = o.status);
  $("ordCount").textContent = view.length;
  $("orders").innerHTML = view.map(o => `
    <div class="order" id="ord-${o.id}">
      <h4>#${esc(o.id.slice(0, 8).toUpperCase())} · ${esc(o.status)}
        <span class="pill">${esc(o.paymentStatus || "")}</span>
        ${o.handoffCode ? `<span class="pill ok">code ${esc(o.handoffCode)}</span>` : ""}</h4>
      <div class="meta">${esc(o.pharmacyName || o.pharmacyId)} · ${esc(o.totalAmount)} · ${esc(o.deliveryAddress || "")}</div>
      <div class="actions">
        ${NEXT[o.status] ? `<button data-adv="${o.id}" data-to="${NEXT[o.status]}">→ ${NEXT[o.status]}</button>` : ""}
        ${o.status !== "DELIVERED" && o.status !== "CANCELLED" ? `<button class="ghost" data-adv="${o.id}" data-to="CANCELLED">Cancel</button>` : ""}
        ${o.paymentStatus === "PENDING" ? `<button class="ghost" data-pay="${o.id}">Mark COD collected</button>` : ""}
      </div>
    </div>`).join("") || "<p class='hint'>No orders.</p>";
  document.querySelectorAll("[data-adv]").forEach(b => b.onclick = () => advanceOrder(b.dataset.adv, b.dataset.to, b));
  document.querySelectorAll("[data-pay]").forEach(b => b.onclick = () => advanceOrder(b.dataset.pay, null, "COD_COLLECTED", b));
  fillSimOrders(view);
}

async function advanceOrder(id, status, paymentStatus, btn) {
  const prev = orderStatusById[id];
  if (btn) btn.disabled = true;
  if (status) { orderStatusById[id] = status; paintOrderStatus(id, status); } // optimistic
  const body = {};
  if (status) body.status = status;
  if (paymentStatus) { body.status = orderStatusById[id] || "CONFIRMED"; body.paymentStatus = paymentStatus; }
  try {
    const r = await fetch(API + "/orders/" + id + "/status", { method: "PUT", headers: auth(), body: JSON.stringify(body) });
    const j = await r.json();
    if (j.success) { toast((status || paymentStatus) + " ok"); }
    else { orderStatusById[id] = prev; toast("Failed: " + (j.error || r.status), true); }
  } catch (e) { orderStatusById[id] = prev; toast("Network: " + e.message, true); }
  loadOrders(true);
}
function paintOrderStatus(id, status) {
  const el = document.querySelector("#ord-" + CSS.escape(id) + " h4");
  if (el) el.firstChild.textContent = "#" + id.slice(0, 8).toUpperCase() + " · " + status + " ";
}

// ---- courier simulator ----
let simPharm = {};
async function fillSimOrders(view) {
  const sel = $("simOrderSel");
  const cur = sel.value;
  sel.innerHTML = "";
  (view.length ? view : orderCache).filter(o => o.status !== "DELIVERED" && o.status !== "CANCELLED").forEach(o => {
    const op = document.createElement("option");
    op.value = o.id; op.textContent = "#" + o.id.slice(0, 8) + " · " + o.status;
    sel.appendChild(op);
  });
  if (!sel.options.length) { const op = document.createElement("option"); op.textContent = "no active orders"; sel.appendChild(op); }
  else if (cur) sel.value = cur;
  // cache pharmacy coords
  try {
    if (!Object.keys(simPharm).length) {
      const r = await fetch(API + "/pharmacies?page=0&pageSize=50", { headers: auth() });
      const j = await r.json();
      (j.data.items || []).forEach(p => simPharm[p.id] = p);
    }
  } catch (e) {}
}
function simPos(orderId, t) {
  const o = orderCache.find(x => x.id === orderId);
  const p = o && simPharm[o.pharmacyId];
  const plat = p ? p.latitude : 31.4495, plng = p ? p.longitude : 34.3925;
  // dropoff ~2km NE of pharmacy (demo stand-in for the customer pin)
  const dlat = plat + 0.018, dlng = plng + 0.018;
  return { lat: plat + (dlat - plat) * t, lng: plng + (dlng - plng) * t,
    eta: Math.max(1, Math.round((1 - t) * 28)), label: (p ? p.name : "pharmacy") + " → customer" };
}
$("simSlider").oninput = () => {
  const t = $("simSlider").value / 100;
  const pos = simPos($("simOrderSel").value, t);
  $("simEta").textContent = "~" + pos.eta + " min · " + pos.label;
};
$("btnSimPush").onclick = async () => {
  const id = $("simOrderSel").value;
  if (!id) return;
  const t = $("simSlider").value / 100;
  const pos = simPos(id, t);
  const r = await fetch(API + "/orders/" + id + "/courier", { method: "PUT", headers: auth(),
    body: JSON.stringify({ lat: pos.lat, lng: pos.lng, etaMinutes: pos.eta }) });
  const j = await r.json().catch(() => ({}));
  if (j.success) toast("Courier pushed · ETA " + pos.eta + "m");
  else toast("Push failed: " + (j.error || r.status), true);
  log("courier " + id.slice(0, 8) + " t=" + t.toFixed(2));
};
$("btnSimClear").onclick = async () => {
  const id = $("simOrderSel").value;
  if (!id) return;
  await fetch(API + "/orders/" + id + "/courier", { method: "PUT", headers: auth(), body: JSON.stringify({ clear: true }) });
  toast("Courier cleared");
};

// ---- prescriptions ----
async function loadPrescs(quiet) {
  let list = [];
  try {
    const r = await fetch(API + "/prescriptions?status=PENDING", { headers: auth() });
    const j = await r.json();
    list = (j.data && j.data.items) || j.data || [];
  } catch (e) { if (!quiet) toast("Rx failed", true); return; }
  $("prescs").innerHTML = list.map(p => `
    <div class="order"><h4>Rx #${esc(p.id.slice(0, 8))} <span class="pill">${esc(p.status || "PENDING")}</span></h4>
      <div class="meta">${esc(p.notes || "")} · ${esc(p.createdAt || "")}</div>
      ${p.imageUrl ? `<img class="rximg" loading="lazy" src="${esc(p.imageUrl)}" alt="prescription" />` : ""}
      <div class="actions">
        <input placeholder="quote" size="8" id="q-${p.id}" />
        <button data-quote="${p.id}">Quote &amp; accept</button>
        <button class="ghost" data-reject="${p.id}">Reject</button>
      </div></div>`).join("") || "<p class='hint'>No pending prescriptions.</p>";
  document.querySelectorAll("[data-quote]").forEach(b => b.onclick = async () => {
    const id = b.dataset.quote, q = document.getElementById("q-" + id).value || "0";
    b.disabled = true;
    await fetch(API + "/prescriptions/" + id + "/status", { method: "PUT", headers: auth(),
      body: JSON.stringify({ status: "RECEIVED_QUOTE", quotedPrice: parseFloat(q) }) });
    const r2 = await fetch(API + "/prescriptions/" + id + "/status", { method: "PUT", headers: auth(),
      body: JSON.stringify({ status: "ACCEPTED" }) });
    toast(r2.ok ? "Quoted & accepted" : "Accept failed", !r2.ok);
    loadPrescs(true);
  });
  document.querySelectorAll("[data-reject]").forEach(b => b.onclick = async () => {
    await fetch(API + "/prescriptions/" + b.dataset.reject + "/status", { method: "PUT", headers: auth(),
      body: JSON.stringify({ status: "REJECTED" }) });
    toast("Rejected");
    loadPrescs(true);
  });
}

// ---- stock ----
async function loadPharmacies() {
  for (const selId of ["pharmSel", "stockPharmSel"]) {
    const sel = $(selId);
    if (sel.options.length) continue;
    try {
      const r = await fetch(API + "/pharmacies?page=0&pageSize=50", { headers: auth() });
      const j = await r.json();
      pharmCache = j.data.items || [];
      pharmCache.forEach(p => {
        const o = document.createElement("option");
        o.value = p.id; o.textContent = p.name + " (" + p.city + ")";
        sel.appendChild(o.cloneNode(true));
      });
    } catch (e) {}
  }
  if ($("stockPharmSel").options.length && !$("stockPharmSel").dataset.bound) {
    $("stockPharmSel").dataset.bound = "1";
    $("stockPharmSel").onchange = loadStock;
    $("medSearch").oninput = debounce(loadStock, 300);
    loadStock();
  }
}
const debounce = (fn, ms) => { let t; return (...a) => { clearTimeout(t); t = setTimeout(() => fn(...a), ms); }; };

async function loadStock() {
  const pid = $("stockPharmSel").value;
  if (!pid) return;
  skel($("stockList"), 2);
  let stock = [];
  try {
    const r = await fetch(API + "/pharmacies/" + pid + "/stock", { headers: auth() });
    stock = (await r.json()).data || [];
  } catch (e) { toast("Stock failed", true); return; }
  const q = $("medSearch").value.trim().toLowerCase();
  let meds = [];
  try {
    const r = await fetch(API + "/medicines/search?q=" + encodeURIComponent(q || "a") + "&page=0&pageSize=20", { headers: auth() });
    const j = await r.json();
    meds = (j.data && j.data.items) || [];
  } catch (e) {}
  const byId = {};
  stock.forEach(s => byId[s.medicineId] = s);
  $("stockList").innerHTML = meds.map(m => {
    const s = byId[m.id] || {};
    return `<div class="order">
      <h4>${esc(m.titleEn)} <span class="pill ${s.isAvailable === false ? "bad" : "ok"}">${s.isAvailable === false ? "hidden" : "visible"}</span></h4>
      <div class="meta">stock ${s.stockQuantity ?? 0} · price ${s.price ?? m.price ?? "—"}</div>
      <div class="actions">
        <input placeholder="qty" size="5" value="${s.stockQuantity ?? 0}" id="sq-${m.id}" />
        <input placeholder="price" size="7" value="${s.price ?? m.price ?? ""}" id="sp-${m.id}" />
        <button data-stock="${m.id}">${s.medicineId ? "Update" : "Add"}</button>
        ${s.medicineId ? `<button class="ghost" data-hide="${m.id}">Hide</button>` : ""}
      </div></div>`;
  }).join("") || "<p class='hint'>No medicines match.</p>";
  document.querySelectorAll("[data-stock]").forEach(b => b.onclick = async () => {
    const mid = b.dataset.stock;
    const qty = parseInt(document.getElementById("sq-" + mid).value || "0", 10);
    const price = parseFloat(document.getElementById("sp-" + mid).value);
    const r = await fetch(API + "/pharmacies/" + pid + "/stock", { method: "PUT", headers: auth(),
      body: JSON.stringify({ medicineId: mid, stockQuantity: qty, price: isNaN(price) ? null : price, isAvailable: qty > 0 }) });
    const j = await r.json().catch(() => ({}));
    toast(j.success ? "Stock saved" : ("Failed: " + (j.error || r.status)), !j.success);
    loadStock();
  });
  document.querySelectorAll("[data-hide]").forEach(b => b.onclick = async () => {
    await fetch(API + "/pharmacies/" + pid + "/stock", { method: "PUT", headers: auth(),
      body: JSON.stringify({ medicineId: b.dataset.hide, isAvailable: false }) });
    toast("Hidden from storefront");
    loadStock();
  });
}

// ---- chats ----
async function loadChats(quiet) {
  let list = [];
  try {
    const r = await fetch(API + "/chats?page=0&pageSize=20", { headers: auth() });
    const j = await r.json();
    list = (j.data && j.data.items) || j.data || [];
  } catch (e) { if (!quiet) toast("Chats failed", true); return; }
  $("chatList").innerHTML = list.map(c => `
    <div class="order" data-chat="${c.id}" style="cursor:pointer">
      <h4>${esc(c.pharmacyName || c.id.slice(0, 8))} ${c.unreadCount ? `<span class="pill bad">${c.unreadCount}</span>` : ""}</h4>
      <div class="meta">${esc(c.lastMessage || "")} · ${esc(c.lastMessageAt || "")}</div>
    </div>`).join("") || "<p class='hint'>No threads.</p>";
  document.querySelectorAll("[data-chat]").forEach(el => el.onclick = () => openChat(el.dataset.chat));
}
async function openChat(id) {
  const box = $("chatMsgs");
  box.style.display = "";
  box.innerHTML = "<p class='hint'>Loading…</p>";
  try {
    const r = await fetch(API + "/chats/" + id + "/messages?page=0&pageSize=20", { headers: auth() });
    const j = await r.json();
    const msgs = (j.data && j.data.items) || j.data || [];
    box.innerHTML = msgs.map(m =>
      `<div class="msg"><b>${esc(m.senderName || m.senderId.slice(0, 6))}:</b> ${esc(m.content || (m.attachmentUrl ? "[image]" : ""))}</div>`
    ).join("") || "<p class='hint'>Empty thread.</p>";
    box.scrollTop = box.scrollHeight;
  } catch (e) { box.innerHTML = "<p class='hint'>Failed to load.</p>"; }
}

// ---- customer ----
async function loadMine(quiet) {
  let list = [];
  try {
    const r = await fetch(API + "/orders", { headers: auth() });
    list = (await r.json()).data || [];
  } catch (e) { if (!quiet) toast("My orders failed", true); return; }
  $("myOrders").innerHTML = list.slice(0, 10).map(o =>
    `<div class="order"><h4>#${esc(o.id.slice(0, 8).toUpperCase())} · ${esc(o.status)}</h4>
     <div class="meta">${esc(o.pharmacyName || "")} · ${esc(o.totalAmount)}</div></div>`).join("") || "<p class='hint'>No orders yet.</p>";
}
$("btnOrder").onclick = async () => {
  if (!$("pharmSel").value) { toast("Pick a pharmacy first", true); return; }
  const body = { prescriptionId: null, pharmacyId: $("pharmSel").value,
    totalAmount: parseFloat($("custTotal").value || "0"),
    paymentMethod: "CASH", deliveryAddress: $("custAddr").value };
  const r = await fetch(API + "/orders", { method: "POST", headers: auth(), body: JSON.stringify(body) });
  const j = await r.json().catch(() => ({}));
  if (j.success) { toast("Order placed: " + j.data.id.slice(0, 8)); const el = document.querySelector("#paneCust"); }
  else toast("Failed: " + (j.error || r.status), true);
  loadMine(true); loadOrders(true);
};

$("statusFilter").onchange = () => loadOrders(true);
$("orderSearch").oninput = debounce(() => loadOrders(true), 300);
paintSession();
if (token) refreshAll();
