package dev.anonymous.eilaji.temp.permission

import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class PermissionHandler(private val fragment: Fragment) {

    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var permissionCallback: ((Boolean) -> Unit)? = null

    fun requestPermission(permission: String, callback: (Boolean) -> Unit) {
        permissionCallback = callback
        permissionLauncher.launch(permission)
    }

    fun handlePermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSION_CODE && grantResults.isNotEmpty()) {
            val isGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
            permissionCallback?.invoke(isGranted)
        }
    }

    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            fragment.requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun registerPermissionLauncher() {
        permissionLauncher = fragment.registerForActivityResult(RequestPermission()) { isGranted ->
            permissionCallback?.invoke(isGranted)
        }
    }



    companion object {
        private const val REQUEST_PERMISSION_CODE = 1001
    }
}
