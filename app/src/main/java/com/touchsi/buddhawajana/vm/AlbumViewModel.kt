package com.touchsi.buddhawajana.vm

import com.touchsi.buddhawajana.api.AlbumJson
import com.touchsi.buddhawajana.entity.AlbumEntity
import com.touchsi.buddhawajana.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow

class AlbumViewModel(repo: AlbumRepository): BaseViewModel<AlbumEntity, AlbumJson>(repo)
{
    val firstLoading = MutableStateFlow(true)
    val selectedIndex = MutableStateFlow(-1)
    val selectedId = MutableStateFlow(0L)
}
