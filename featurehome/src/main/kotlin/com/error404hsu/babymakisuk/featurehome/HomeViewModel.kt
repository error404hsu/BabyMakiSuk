package com.error404hsu.babymakisuk.featurehome

import androidx.lifecycle.ViewModel
import com.error404hsu.babymakisuk.coredata.repository.ChildRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val childRepository: ChildRepository
) : ViewModel() {
    val children = childRepository.observeAll()

    fun onAddChildClick() {
        // TODO: navigate to AddChildScreen
    }
}
