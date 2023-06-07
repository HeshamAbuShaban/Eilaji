package dev.anonymous.eilaji.temp.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun requestPermission(permission: String, requestCode: Int, rationale: String?, onPermissionResult: (Boolean) -> Unit) {
        if (hasPermission(permission)) {
            onPermissionResult(true)
        } else {
            // notify the user to activate them
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (shouldShowRequestPermissionRationale(Activity(), permission) && rationale != null) {
                    // Show rationale to the user
                    // You can display a dialog or a snackBar explaining why the permission is needed
                    // and provide an option to request the permission again
                    Toast.makeText(context, "SomePermission Must be Added!", Toast.LENGTH_LONG).show();
                }
                ActivityCompat.requestPermissions(context as Activity, arrayOf(permission), requestCode)
            }
            onPermissionResult(false)
        }
    }

    fun handlePermissionsResult(requestCode: Int, permissions: Array<String>,
                                grantResults: IntArray, onPermissionResult: (Boolean) -> Unit) {
        if (requestCode == REQUEST_CODE && permissions.isNotEmpty()) {
            val permission = permissions[0]
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionResult(true)
            } else {
                onPermissionResult(false)
            }
        }
    }

    companion object {
        const val REQUEST_CODE = 123 // Use any unique value for the request code
    }
}
