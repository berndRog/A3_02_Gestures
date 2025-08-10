package de.rogallab.mobile.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.rogallab.mobile.ui.base.BaseActivity
import de.rogallab.mobile.ui.people.composables.list.PeopleListScreen
import de.rogallab.mobile.ui.theme.AppTheme

class MainActivity : BaseActivity(TAG) {

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      enableEdgeToEdge()
      setContent {

         AppTheme {
//            PersonInputScreen()
//            PersonDetailScreen(
//               id = "db6cee2b-5f90-459a-aabe-876ef80fcd5f"
//            )
            PeopleListScreen()
         }
      }
   }

   companion object {
      private const val TAG = "<-MainActivity"
   }
}


private fun isInTest(): Boolean {
   return try {
      Class.forName("androidx.test.espresso.Espresso")
      true
   } catch (e: ClassNotFoundException) {
      false
   }
}

