package de.rogallab.mobile.ui.people.composables.list
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.ui.people.PersonIntent
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipePersonListItem(
   person: Person,
   onNavigate: (String) -> Unit, // This should trigger viewModel.prepareAndNavigateToDetail
   onProcessIntent: (PersonIntent) -> Unit, // For delete action
   onErrorEvent: () -> Unit, // Ask user to undo
   onUndoAction: () -> Unit, // For undoing the delete
   animationDuration: Int = 1000,
   content: @Composable () -> Unit
) {
   val tag = "<-SwipePersonListItem"

   var isRemoved by remember { mutableStateOf(false) }
   // This flag ensures onNavigate is called only once per successful swipe-to-edit gesture.
   // It's reset when the item settles back.
   var hasNavigatedThisSwipe by remember { mutableStateOf(false) }

   val state: SwipeToDismissBoxState =
      rememberSwipeToDismissBoxState(
         initialValue = SwipeToDismissBoxValue.Settled,
         confirmValueChange = { targetValue: SwipeToDismissBoxValue ->
            when (targetValue) {
               SwipeToDismissBoxValue.StartToEnd -> { // Swipe to Edit/Navigate
                  if (!hasNavigatedThisSwipe) {
                     logDebug(tag, "Swipe to Edit confirmed for ${person.id}. Calling onNavigate.")
                     onNavigate(person.id)
                     hasNavigatedThisSwipe = true // Mark that navigation has been attempted for this gesture
                     return@rememberSwipeToDismissBoxState true // Confirm the state change
                  }
                  logDebug(tag,"Swipe to Edit: Navigation already attempted this swipe for ${person.id}.")
                  return@rememberSwipeToDismissBoxState false // Don't re-confirm if already attempted
               }
               SwipeToDismissBoxValue.EndToStart -> { // Swipe to Delete
                  logDebug(tag, "Swipe to Delete confirmed for ${person.id}.")
                  isRemoved = true
                  return@rememberSwipeToDismissBoxState true // Confirm the state change
               }
               SwipeToDismissBoxValue.Settled -> {
                  // This case is typically handled by the swipe itself resetting,
                  // but we manage hasNavigatedThisSwipe in a LaunchedEffect based on currentValue.
                  logDebug(tag, "Swipe: Item settled for ${person.id}.")
                  return@rememberSwipeToDismissBoxState true // Allow settling
               }
            }
         },
         positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold,
      )

   // Reset hasNavigatedThisSwipe when the item settles back to its original position.
   // This is crucial if a navigation attempt was made (hasNavigatedThisSwipe = true)
   // but the actual navigation was prevented by the ViewModel (e.g., pre-check failed).
   // Resetting allows a new navigation attempt on a subsequent swipe.
   LaunchedEffect(state.currentValue) {
      if (state.currentValue == SwipeToDismissBoxValue.Settled) {
         if (hasNavigatedThisSwipe) {
            logDebug(tag, "Item settled. Resetting hasNavigatedThisSwipe for ${person.id}.")
            hasNavigatedThisSwipe = false
         }
      }
   }

   val undoDeletePersonMessage = stringResource(R.string.undoDeletePerson)
   val undoActionLabel = stringResource(R.string.undoAnswer)

   // Effect for handling the removal process (animation and intent)
   LaunchedEffect(key1 = isRemoved) {
      if (isRemoved) {
         delay(animationDuration.toLong()) // Wait for visual animation
         logDebug(tag, "Start remove for ${person.id}")
         // onProcessIntent(PersonIntent.Remove(person)) // Inform ViewModel to remove the person

         // Prepare Snackbar for undoing the delete
         logDebug(tag,"Starting delete visual feedback and logic")
         onErrorEvent() // Ask user to undo action
      }
   }

   AnimatedVisibility(
      visible = !isRemoved, // Item is visible unless it's in the process of being removed
      exit = shrinkVertically(
         animationSpec = tween(durationMillis = animationDuration),
         shrinkTowards = Alignment.Top
      ) + fadeOut(),
      modifier = Modifier // Add any specific modifiers for AnimatedVisibility if needed
   ) {
      SwipeToDismissBox(
         state = state,
         backgroundContent = { SetSwipeBackground(state) }, // Assumed this composable is defined elsewhere
         modifier = Modifier.padding(vertical = 4.dp),
         enableDismissFromStartToEnd = true, // Allows swipe from left-to-right (e.g., Edit)
         enableDismissFromEndToStart = true  // Allows swipe from right-to-left (e.g., Delete)
      ) {
         content() // This is the actual UI content of the list item (e.g., PersonCard)
      }
   }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SetSwipeBackground(state: SwipeToDismissBoxState) {

   // Determine the properties of the swipe
   val (colorBox, colorIcon, alignment, icon, description, scale) =
      getSwipeProperties(state)

   Box(
      Modifier.fillMaxSize()
         .background(
            color = colorBox,
            shape = RoundedCornerShape(10.dp)
         )
         .padding(horizontal = 16.dp),
      contentAlignment = alignment
   ) {
      Icon(
         icon,
         contentDescription = description,
         modifier = Modifier.scale(scale),
         tint = colorIcon
      )
   }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun getSwipeProperties(
   state: SwipeToDismissBoxState
): SwipeProperties {

   // Set the color of the box
   // https://hslpicker.com
   val colorBox: Color = when (state.targetValue) {
      SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surface
      SwipeToDismissBoxValue.StartToEnd -> Color.hsl(120.0f,0.80f,0.30f, 1f) //Color.Green    // move to right
      // move to left  color: dark red
      SwipeToDismissBoxValue.EndToStart -> Color.hsl(0.0f,0.90f,0.40f,1f)//Color.Red      // move to left
   }

   // Set the color of the icon
   val colorIcon: Color = when (state.targetValue) {
      SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.onSurface
      else -> Color.White
   }

   // Set the alignment of the icon
   val alignment: Alignment = when (state.dismissDirection) {
      SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
      SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
      else -> Alignment.Center
   }

   // Set the icon
   val icon: ImageVector = when (state.dismissDirection) {
      SwipeToDismissBoxValue.StartToEnd -> Icons.Outlined.Edit   // left
      SwipeToDismissBoxValue.EndToStart -> Icons.Outlined.Delete // right
      else -> Icons.Outlined.Info
   }

   // Set the description
   val description: String = when (state.dismissDirection) {
      SwipeToDismissBoxValue.StartToEnd -> "Editieren"
      SwipeToDismissBoxValue.EndToStart -> "Löschen"
      else -> "Unknown Action"
   }

   // Set the scale
   val scale = if (state.targetValue == SwipeToDismissBoxValue.Settled)
      1.2f else 1.8f

   return SwipeProperties(
      colorBox, colorIcon, alignment, icon, description, scale)
}

data class SwipeProperties(
   val colorBox: Color,
   val colorIcon: Color,
   val alignment: Alignment,
   val icon: ImageVector,
   val description: String,
   val scale: Float
)