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
      logDebug(TAG, "fetchById() $id")
      _repository.findById(id)
         .onSuccess { person ->
            _personUiStateFlow.update { it: PersonUiState ->
               it.copy(person = person ?: Person()) // if null, create an empty
            }
         }
         .onFailure { logError(TAG, it.message ?: "Error in fetchById") }
   }

   private fun create() {
      logDebug(TAG, "createPerson")
      _repository.create(_personUiStateFlow.value.person)
         .onSuccess { fetch() } // reread all people
         .onFailure { logError(TAG, it.message ?: "Error in create") }
   }

   private fun update() {
      logDebug(TAG, "updatePerson()")
      _repository.update(_personUiStateFlow.value.person)
         .onSuccess { fetch() } // reread all people
         .onFailure { logError(TAG, it.message ?: "Error in update") }
   }

   private var removedPerson: Person? = null
   private fun remove(person: Person) {
      logDebug(TAG, "removePerson()")
      removedPerson = person
      _repository.remove(person)
         .onSuccess { fetch() } // reread all people
         .onFailure { logError(TAG, it.message ?: "Error in remove") }
   }

   private fun undoRemove() {
      removedPerson?.let { person ->
         logDebug(TAG, "undoRemovePerson: ${person.id.as8()}")
         _repository.create(person)
            .onSuccess {
               _personUiStateFlow.update { it: PersonUiState ->
                  it.copy(person = person)  // new UiState
               }
               removedPerson = null
               fetch()
            }
            .onFailure { t -> logError(TAG, t.message ?: "Error in remove")  }
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

   // read all people from repository
   private fun fetch() {
      logDebug(TAG, "fetch")
      _repository.getAll()
         .onSuccess { people ->
            _peopleUiStateFlow.update { it: PeopleUiState ->
               it.copy(people = emptyList())
               it.copy(people = people)
            }
         }
         .onFailure { logError(TAG, it.message ?: "Error in fetch") }
   }
   // endregion

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}