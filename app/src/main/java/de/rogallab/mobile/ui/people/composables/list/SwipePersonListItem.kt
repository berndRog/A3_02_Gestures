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
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.logDebug // Make sure this utility is available
import kotlinx.coroutines.delay


/**
 * SwipePersonListItem Algorithm
 *
 * OVERVIEW:
 * This composable implements a swipeable list item with delete/undo functionality and edit navigation.
 * It uses a single state variable approach to manage swipe gestures and their corresponding actions.
 *
 * STATE MANAGEMENT:
 * - isDelete: Boolean - Single state controlling both visual animation and delete logic
 *
 * SWIPE GESTURE HANDLING:
 * SwipeToDismissBoxState.confirmValueChange() validates and confirms swipe actions:
 * 1. StartToEnd (left-to-right swipe): EDIT ACTION
 *    - Immediately calls onNavigate(person.id) for navigation to edit screen
 *    - Returns false to snap item back to original position (no dismissal)
 *    - Action fires once per swipe gesture
 *
 * 2. EndToStart (right-to-left swipe): DELETE ACTION
 *    - Sets isDelete = true (triggers both visual animation and delete sequence)
 *    - Returns false to prevent actual dismissal (we handle it manually)
 *
 * 3. Settled: Returns true to allow normal position reset
 *
 * DELETE SEQUENCE (LaunchedEffect with isDelete key):
 * When isDelete becomes true:
 * 1. Wait for visual exit animation to complete (delay(animationDuration))
 * 2. Execute onDelete() - removes person from data store
 * 3. Execute onUndo() - displays snackbar with undo option
 * 4. NOTE: isDelete state is NOT reset here - handled by undo restoration effect
 *
 * UNDO RESTORATION (LaunchedEffect with person.id key):
 * Monitors when person exists in composable but isDelete = true:
 * - This condition indicates the person was restored via undo action
 * - Adds small delay (100ms) to ensure data restoration is complete
 * - Resets isDelete = false to restore normal visual state
 * - Makes the item visible again by triggering AnimatedVisibility recomposition
 *
 * VISUAL ANIMATION:
 * - AnimatedVisibility with visible = !isDelete controls item visibility
 * - Exit animation: shrinkVertically (top-aligned) + fadeOut over animationDuration
 * - SwipeToDismissBox provides colored backgrounds and icons based on swipe direction
 *
 * KEY DESIGN PATTERNS:
 * - Single state variable: isDelete serves dual purpose (visual + logic trigger)
 * - Immediate actions: Edit navigation happens instantly on swipe confirmation
 * - Deferred actions: Delete sequence waits for animation completion
 * - Undo detection: Monitors person existence vs isDelete state mismatch
 * - Animation synchronization: Visual and data operations properly coordinated
 * - State persistence: isDelete remains true until explicit undo restoration
 *
 * ADVANTAGES:
 * - Simplified state management with single boolean
 * - Clear separation between immediate (edit) and deferred (delete) actions
 * - Proper undo handling with state restoration
 * - Smooth visual animations synchronized with data operations
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipePersonListItem(
   person: Person,
   onEdit: (String) -> Unit,
   onDelete: () -> Unit,
   onUndo: () -> Unit,
   animationDuration: Int = 500,
   content: @Composable () -> Unit
) {
   val tag = "<-SwipePersonListItem"
   var isDelete by remember { mutableStateOf(false) }

   val state = rememberSwipeToDismissBoxState(
      initialValue = SwipeToDismissBoxValue.Settled,
      confirmValueChange = { target ->
         when (target) {
            SwipeToDismissBoxValue.StartToEnd -> {
               logDebug(tag, "Swipe to Edit for ${person.firstName+" "+person.lastName}")
               // Edit: fire once, snap back by returning false
               onEdit(person.id)
               false
            }
            SwipeToDismissBoxValue.EndToStart -> {
               logDebug(tag, "Swipe to Delete for ${person.firstName+" "+person.lastName}")
               // Delete: start exit animation, then handle delete+undo in effect
               isDelete = true
               false
            }
            SwipeToDismissBoxValue.Settled -> true
         }
      }
   )

   // When exit animation starts, finish the delete flow.
   LaunchedEffect(isDelete) {
      if (isDelete) {
         delay(animationDuration.toLong())
         logDebug(tag, "Delete person ${person.firstName+" "+person.lastName}")
         onDelete()
         logDebug(tag, "Show undo snackbar")
         onUndo() // show snackbar with undo
      }
   }

   // For a proper undo handling
   LaunchedEffect(person.id) {
      if (isDelete) {
         logDebug(tag, "Person ${person.firstName+" "+person.lastName} was restored via undo")
         delay(100) // Allow data restoration to complete
         isDelete = false // Reset visual state when person is restored
      }
   }

   AnimatedVisibility(
      visible = !isDelete,
      exit = shrinkVertically(
         animationSpec = tween(durationMillis = animationDuration),
         shrinkTowards = Alignment.Top
      ) + fadeOut()
   ) {
      SwipeToDismissBox(
         state = state,
         backgroundContent = { SetSwipeBackground(state) },
         enableDismissFromStartToEnd = true,  // edit
         enableDismissFromEndToStart = true,  // delete
         modifier = Modifier.padding(vertical = 4.dp)
      ) {
         content()
      }
   }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SetSwipeBackground(state: SwipeToDismissBoxState) {

   // Determine the properties of the swipe
   val (colorBox, colorIcon, alignment, icon, description, scale) =
      GetSwipeProperties(state)

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
fun GetSwipeProperties(
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