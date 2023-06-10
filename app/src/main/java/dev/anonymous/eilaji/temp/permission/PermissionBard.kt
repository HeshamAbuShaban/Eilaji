package dev.anonymous.eilaji.temp.permission

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PermissionBard(private val activity: AppCompatActivity) {
    // Define a function to request a permission
    fun requestPermission(
        permission: String,
        isGrantedBody: () -> Unit,
        isNotGrantedBody: () -> Unit
    ) {
        // Create an ActivityResultLauncher for the permission request
        val permissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission())
            { isGranted ->
                // Handle the result of the permission request
                if (isGranted) {
                    // Permission granted, do something
                    isGrantedBody()
                } else {
                    // Permission denied, do something else
                    isNotGrantedBody()
                }
            }

        // Request the permission
        permissionLauncher.launch(permission)
    }

    // Define a function to show a permission dialog
    fun showPermissionDialog(permission: String) {
        // Create a AlertDialogBuilder
        val builder = AlertDialog.Builder(activity)

        // Set the title of the dialog
        builder.setTitle("Permission required")

        // Set the message of the dialog
        builder.setMessage("This app needs access to $permission")

        // Set the positive button of the dialog
        builder.setPositiveButton("OK") { _, _ ->
            // Request the permission
            requestPermission(permission, {}, {})
        }

        // Set the negative button of the dialog
        builder.setNegativeButton("Cancel") { _, _ ->
            // Do nothing
        }

        // Create and show the dialog
        val dialog = builder.create()
        dialog.show()
    }

}