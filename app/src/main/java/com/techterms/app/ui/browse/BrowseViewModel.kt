package com.techterms.app.ui.browse

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.techterms.app.data.db.Term
import com.techterms.app.data.repository.TermRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BrowseUiState(
    val query: String = "",
    val selectedCategory: String? = null,
    val devOnly: Boolean = false,
    val terms: List<Term> = emptyList(),
    val isLoading: Boolean = true
)

private data class BrowseFilter(
    val query: String,
    val category: String?,
    val devOnly: Boolean
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class BrowseViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val termRepository: TermRepository
) : ViewModel() {

    private val queryFlow: StateFlow<String> =
        savedStateHandle.getStateFlow(KEY_QUERY, "")

    private val selectedCategoryFlow: StateFlow<String?> =
        savedStateHandle.getStateFlow(KEY_CATEGORY, null)

    private val devOnlyFlow: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(KEY_DEV_ONLY, false)

    private val isSeedReadyFlow = MutableStateFlow(false)

    private val filteredTermsFlow = combine(
        queryFlow.debounce { if (it.isBlank()) 0L else 250L },
        selectedCategoryFlow,
        devOnlyFlow
    ) { debouncedQuery, category, devOnly ->
        BrowseFilter(debouncedQuery, category, devOnly)
    }.flatMapLatest { filter ->
        if (filter.query.isBlank() && filter.category == null && !filter.devOnly) {
            termRepository.getAllTerms()
        } else {
            termRepository.searchTerms(filter.query, filter.category, filter.devOnly)
        }
    }.catch { e ->
        Log.e(TAG, "Failed to query terms in BrowseViewModel", e)
        emit(emptyList())
    }

    val uiState: StateFlow<BrowseUiState> = combine(
        queryFlow,
        selectedCategoryFlow,
        devOnlyFlow,
        filteredTermsFlow,
        isSeedReadyFlow
    ) { query, category, devOnly, terms, isReady ->
        BrowseUiState(
            query = query,
            selectedCategory = category,
            devOnly = devOnly,
            terms = terms,
            isLoading = !isReady && terms.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = BrowseUiState()
    )

    init {
        viewModelScope.launch {
            try {
                termRepository.ensurePopulated()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to ensure terms populated in BrowseViewModel", e)
            } finally {
                isSeedReadyFlow.value = true
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        savedStateHandle[KEY_QUERY] = newQuery
    }

    fun onCategorySelected(category: String?) {
        savedStateHandle[KEY_CATEGORY] = category
    }

    fun onDevOnlyToggle() {
        savedStateHandle[KEY_DEV_ONLY] = !devOnlyFlow.value
    }

    companion object {
        private const val TAG = "TechTerms"
        private const val KEY_QUERY = "browse_query"
        private const val KEY_CATEGORY = "browse_category"
        private const val KEY_DEV_ONLY = "browse_dev_only"

        fun provideFactory(
            termRepository: TermRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                return BrowseViewModel(
                    savedStateHandle = extras.createSavedStateHandle(),
                    termRepository = termRepository
                ) as T
            }
        }
    }
}
