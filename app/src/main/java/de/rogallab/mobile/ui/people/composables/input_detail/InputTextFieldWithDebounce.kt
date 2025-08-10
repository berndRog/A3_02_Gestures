package de.rogallab.mobile.ui.people.composables.input_detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.logVerbose
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
Common input validation patterns in Jetpack Compose include:
1. Immediate Feedback: Validate input as the user types and provide immediate feedback.
2. Debouncing: Delay validation until the user stops typing to avoid excessive recompositions.
3. Single Source of Truth: Maintain input state and validation state in a ViewModel or higher-level composable.
4. Reusable Validation Functions: Create reusable functions for validation logic.
5. Derived State: Use `derivedStateOf` to derive validation state from input state.
6. Visual Cues: Use visual indicators like color changes, icons, and error messages to indicate validation errors.
7. Accessibility: Ensure error messages and input fields are accessible to screen readers.
*/
@Composable
fun InputTextFieldWithDebounce(
   value: String,                                  // State ↓
   onValueChange: (String) -> Unit,                // Event ↑
   label: String,                                  // State ↓
   validationOnDebounce: Boolean = false,          // State ↓
   validate: (String) -> Pair<Boolean, String>,    // Event ↑
   leadingIcon: @Composable (() -> Unit)? = null,  // Composable ↑
   keyboardOptions: KeyboardOptions = KeyboardOptions.Default, // State ↓
   imeAction: ImeAction = ImeAction.Done,          // State ↓
) {
   // local state for the OutLinedTextField
   var localValue by rememberSaveable { mutableStateOf(value) }
   // local state for focus (i.e. is the text field focused?)
   var isFocus by rememberSaveable { mutableStateOf(false) }
   // local state for error handling
   var isError by rememberSaveable { mutableStateOf(false) }
   var errorText by rememberSaveable { mutableStateOf("") }

   // Coroutine scope to launch the debounce job (parallel execution)
   val coroutineScope = rememberCoroutineScope()
   // debounce job to delay the onNameChange event
   var debounceJob: Job? by remember { mutableStateOf(null) }

   val focusManager = LocalFocusManager.current

   // Update localValue when value changes
   LaunchedEffect(value) {
      if (value != localValue) localValue = value
   }

   // Validate the input when focus is lost
   fun validateAndPropagate() {
      val (error, text) = validate(localValue)
      isError = error
      errorText = text
      if (!isError && localValue != value) onValueChange(localValue)
   }

   // Debounce the value change to avoid immediate validation
   LaunchedEffect(localValue) {
      delay(500) // wait 250 ms
      if (localValue != value) {
         // propagate change with validation
         if(validationOnDebounce) validateAndPropagate()
         // propagate change without validation
         else onValueChange(localValue)
      }
   }
   OutlinedTextField(
      modifier = Modifier
         .fillMaxWidth()
         .onFocusChanged { focusState ->
            // logVerbose("<-InputTextField","onFocusChanged !focusState.isFocused ${!focusState.isFocused} isFocus $isFocus")
            if (!focusState.isFocused && isFocus) {
               validateAndPropagate()
               debounceJob?.cancel()
            }
            isFocus = focusState.isFocused
         },
      // Set the text field's local state value
      value = localValue,
      // Handle value changes of the local state
      onValueChange = {
         localValue = it
         if (isError) {
            isError = false
            errorText = ""
         }
         // Cancel the previous debounce job if it exists
         debounceJob?.cancel()
         // Start a new debounce job
         debounceJob = coroutineScope.launch {
            delay(300) // wait 500 ms
            logDebug("<-InputTextField", "debounce: value:$value localValue:$localValue")
            if (localValue != value) {
               logDebug("<-InputTextField", "debounce, onValueChange:$localValue")
               if(validationOnDebounce) validateAndPropagate()
               else onValueChange(localValue)
            }
         }
      },
      // Set the label of the text field
      label = { Text(label) },
      textStyle = MaterialTheme.typography.bodyLarge,
      // Add leading icon to the text field
      leadingIcon = leadingIcon,
      // Ensure the text field is single line
      singleLine = true,
      // Set keyboard options for the text field
      keyboardOptions = keyboardOptions.copy(imeAction = imeAction),
      // Set keyboard actions for the text field
      keyboardActions = KeyboardActions(
         onAny = {
            validateAndPropagate()
            if (!isError) focusManager.clearFocus()
         }
      ),
      // Is there an error?
      isError = isError,
      // Provide supporting text and trailing icon if there is an error
      supportingText = {
         if (isError) Text(
            text = errorText,
            color = MaterialTheme.colorScheme.error
         )
      },
      trailingIcon = {
         if (isError) Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = errorText,
            tint = MaterialTheme.colorScheme.error
         )
      }
   )
}