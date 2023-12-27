package com.watnapp.buddhawajana.vm

import com.watnapp.buddhawajana.api.AlbumJson
import com.watnapp.buddhawajana.entity.AlbumEntity
import com.watnapp.buddhawajana.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow

class AlbumViewModel(repo: AlbumRepository): BaseViewModel<AlbumEntity, AlbumJson>(repo)
{
    val firstLoading = MutableStateFlow(true)
    val selectedIndex = MutableStateFlow(-1)
    val selectedId = MutableStateFlow(0L)
}
