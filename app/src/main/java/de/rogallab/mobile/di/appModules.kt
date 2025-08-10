package de.rogallab.mobile.di

import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.data.local.datastore.DataStore
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.utilities.logInfo
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.PersonViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val defineModules: Module = module {
    val tag = "<-dataModules"

    logInfo(tag, "single    -> Seed")
    single<Seed> {
        Seed(
           _context = androidContext(),
        )
    }

    logInfo(tag, "single    -> DataStore")
    single {
        DataStore(
           _context = androidContext(),
        )
    }

    logInfo(tag, "single    -> PersonRepository: IPersonRepository")
    single<IPersonRepository> {
        PersonRepository(
           _dataStore = get<DataStore>(),
        )
    }

    logInfo(tag, "single    -> PersonValidator")
    single<PersonValidator> {
        PersonValidator(
           _context = androidContext()
        )
    }

    logInfo(tag, "viewModel -> PersonViewModel")
    viewModel {
        PersonViewModel(
           _repository = get<IPersonRepository>(),
           _validator = get<PersonValidator>()
        )
    }
}

val appModules: Module = module {

    try {
        val testedModules = defineModules
        requireNotNull(testedModules) {
            "dataModules is null"
        }


        includes(
           testedModules
           //useCaseModules
        )

    } catch (e: Exception) {
        logInfo("<-appModules", e.message!!)
    }
}