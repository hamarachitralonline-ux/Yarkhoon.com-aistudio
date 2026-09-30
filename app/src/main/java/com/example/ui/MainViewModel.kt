package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Article
import com.example.data.CulturalFeature
import com.example.data.ValleyPlace
import com.example.data.YarkhoonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val articles: List<Article> = emptyList(),
    val places: List<ValleyPlace> = emptyList(),
    val cultureFeatures: List<CulturalFeature> = emptyList(),
    val isLoading: Boolean = false,
    val selectedArticle: Article? = null
)

class MainViewModel(
    private val repository: YarkhoonRepository = YarkhoonRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val articles = repository.fetchArticles()
            val places = repository.getValleyPlaces()
            val culture = repository.getCulturalFeatures()
            _uiState.value = UiState(
                articles = articles,
                places = places,
                cultureFeatures = culture,
                isLoading = false
            )
        }
    }

    fun selectArticle(article: Article?) {
        _uiState.value = _uiState.value.copy(selectedArticle = article)
    }
}
