package de.rogallab.mobile.ui.people

import androidx.lifecycle.ViewModel
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.ResultData
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.as8
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.logError
import de.rogallab.mobile.domain.utilities.newUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PersonViewModel(
   private val _repository: IPersonRepository,
   private val _validator: PersonValidator
): ViewModel() {

   //region PersonInput-/PersonDetailScreen
   // StateFlow for UIState Person
   private val _personUiStateFlow = MutableStateFlow(PersonUiState())
   val personUiStateFlow = _personUiStateFlow.asStateFlow()

   // transform intent into an action
   fun onProcessPersonIntent(intent: PersonIntent) {
      when (intent) {
         is PersonIntent.FirstNameChange -> onFirstNameChange(intent.firstName)
         is PersonIntent.LastNameChange -> onLastNameChange(intent.lastName)
         is PersonIntent.EmailChange -> onEmailChange(intent.email)
         is PersonIntent.PhoneChange -> onPhoneChange(intent.phone)

         is PersonIntent.Clear -> clearState()
         is PersonIntent.FetchById -> fetchById(intent.id)
         is PersonIntent.Create -> create()
         is PersonIntent.Update -> update()
         is PersonIntent.Remove -> remove(intent.person)

         PersonIntent.Undo -> undoRemove()
      }
   }

   private fun onFirstNameChange(firstName: String) {
      val trimmed = firstName.trim()
      if (trimmed == _personUiStateFlow.value.person.firstName) return
      _personUiStateFlow.update { it: PersonUiState ->
         it.copy(person = it.person.copy(firstName = trimmed))
      }
   }
   private fun onLastNameChange(lastName: String) {
      val trimmed = lastName.trim()
      if (trimmed == _personUiStateFlow.value.person.lastName) return
      _personUiStateFlow.update { it: PersonUiState ->
         it.copy(person = it.person.copy(lastName = trimmed))
      }
   }
   private fun onEmailChange(email: String?) {
      val trimmed = email?.trim()
      if (trimmed == _personUiStateFlow.value.person.email) return
      _personUiStateFlow.update { it: PersonUiState ->
         it.copy(person = it.person.copy(email = trimmed))
      }
   }
   private fun onPhoneChange(phone: String?) {
      val trimmed = phone?.trim()
      if(trimmed == _personUiStateFlow.value.person.phone) return
      _personUiStateFlow.update { it: PersonUiState ->
         it.copy(person = it.person.copy(phone = trimmed))
      }
   }

   private fun clearState() {
      _personUiStateFlow.update { it.copy(person = Person(id = newUuid() )) }
   }

   private fun fetchById(id: String) {
      logDebug(TAG, "fetchPersonById: $id")
      when (val resultData = _repository.getById(id)) {
         is ResultData.Success -> {
            _personUiStateFlow.update { it: PersonUiState ->
               it.copy(person = resultData.data ?: Person(id = newUuid()))  // new UiState
            }
         }
         is ResultData.Error ->
            logError(TAG, resultData.throwable.message ?: "Error fetchById")
      }
   }
   private fun create() {
      logDebug(TAG, "createPerson")
      when (val resultData = _repository.create(_personUiStateFlow.value.person)) {
         is ResultData.Success -> {}
         is ResultData.Error ->
            logError(TAG, resultData.throwable.message ?: "Error in create")
      }
   }
   private fun update() {
      logDebug(TAG, "updatePerson")
      when (val resultData = _repository.update(_personUiStateFlow.value.person)) {
         is ResultData.Success -> {}
         is ResultData.Error ->
            logError(TAG, resultData.throwable.message ?: "Error in update")
      }
   }

   private var removedPerson: Person? = null
   private fun remove(person: Person) {
      removedPerson = person
      logDebug(TAG, "removePerson: $person")
      when (val resultData = _repository.remove(person)) {
         is ResultData.Success -> {}
         is ResultData.Error ->
            logError(TAG, resultData.throwable.message ?: "Error in remove")
      }
   }
   private fun undoRemove() {
      removedPerson?.let { person ->
         logDebug(TAG, "undoRemovePerson: ${person.id.as8()}")
         when(val resultData = _repository.create(person)) {
            is ResultData.Success -> {
               removedPerson = null
               fetch()
            }
            is ResultData.Error ->
               logError(TAG, resultData.throwable.message ?: "Error in undoRemove")
         }
      }
   }

   // validate all input fields after user finished input into the form
   fun validate(): Boolean {
      val person = _personUiStateFlow.value.person

      // only one error message can be processed at a time
      if(!validateAndLogError(_validator.validateFirstName(person.firstName)))
         return false
      if(!validateAndLogError(_validator.validateLastName(person.lastName)))
         return false
      if(!validateAndLogError(_validator.validateEmail(person.email)))
         return false
      if(!validateAndLogError(_validator.validatePhone(person.phone)))
         return false
      return true // all fields are valid
   }

   private fun validateAndLogError(validationResult: Pair<Boolean, String>): Boolean {
      val (error, message) = validationResult
      if (error) {
         logError(TAG, message)
         return false
      }
      return true
   }

   // region PeopleListScreen
   // StateFlow for UI State People
   private val _peopleUiStateFlow = MutableStateFlow(PeopleUiState())
   val peopleUiStateFlow = _peopleUiStateFlow.asStateFlow()

   // transform intent into an action
   fun onProcessPeopleIntent(intent: PeopleIntent) {
      when (intent) {
         is PeopleIntent.Fetch -> fetch()
      }
   }

   private fun fetch() {
      when (val resultData = _repository.getAll()) {
         is ResultData.Success -> {
            logDebug(TAG, "fetch() people.size: ${resultData.data.size}")
            _peopleUiStateFlow.update { it: PeopleUiState ->

               if(it.people.toList() == resultData.data.toList()) {
                  // clear existing list
                  logDebug(TAG, "fetch() no changes, skipping update")
               }
               it.copy(people = resultData.data.toList())
            }
         }
         is ResultData.Error ->
            logError(TAG, resultData.throwable.message ?: "Error fetch")
      }
   }
   // endregion


   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}