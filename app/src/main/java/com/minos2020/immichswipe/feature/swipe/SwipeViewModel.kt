package com.minos2020.immichswipe.feature.swipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minos2020.immichswipe.data.repository.AssetRepository
import com.minos2020.immichswipe.core.PlaybackBehavior
import com.minos2020.immichswipe.core.IconPosition
import com.minos2020.immichswipe.data.repository.SessionRepository
import com.minos2020.immichswipe.data.repository.SwipeDecisionRepository
import com.minos2020.immichswipe.domain.model.Album
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class SwipeViewModel(
    private val assetRepository: AssetRepository,
    private val sessionRepository: SessionRepository,
    private val swipeDecisionRepository: SwipeDecisionRepository,
    private val album: Album
) : ViewModel() {

    private val _uiState = MutableStateFlow(SwipeUiState(albumName = album.albumName))
    val uiState: StateFlow<SwipeUiState> = _uiState.asStateFlow()

    init {
        loadAssetsAndDecisions()
        observePlaybackBehavior()
        observeSwipeInversion()
        observeFullscreenButtonPosition()
        observeImmichButtonPosition()
        observeSkipLifespan()
    }

    private fun observeSkipLifespan() {
        viewModelScope.launch {
            sessionRepository.skipLifespanDays.collect { days ->
                _uiState.value = _uiState.value.copy(skipLifespanDays = days)
            }
        }
    }

    /**
     * Retente le chargement des données si une erreur a eu lieu.
     */
    fun retryLoading() {
        if (!_uiState.value.isLoading) {
            _uiState.value = _uiState.value.copy(error = null)
            loadAssetsAndDecisions()
        }
    }

    private fun observePlaybackBehavior() {
        viewModelScope.launch {
            sessionRepository.playbackBehavior.collect { behavior ->
                _uiState.value = _uiState.value.copy(playbackBehavior = behavior)
            }
        }
    }

    private fun observeSwipeInversion() {
        viewModelScope.launch {
            sessionRepository.swipeInverted.collect { inverted ->
                _uiState.value = _uiState.value.copy(isSwipeInverted = inverted)
            }
        }
    }

    private fun observeFullscreenButtonPosition() {
        viewModelScope.launch {
            sessionRepository.fullscreenButtonPosition.collect { pos ->
                _uiState.value = _uiState.value.copy(fullscreenButtonPosition = pos)
            }
        }
    }

    private fun observeImmichButtonPosition() {
        viewModelScope.launch {
            sessionRepository.immichButtonPosition.collect { pos ->
                _uiState.value = _uiState.value.copy(immichButtonPosition = pos)
            }
        }
    }

    private fun loadAssetsAndDecisions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // On charge les assets depuis l'API
                val assets = assetRepository.getAssetsByAlbum(album.id)
                
                // On charge les décisions locales déjà prises pour cet album
                // On utilise first() pour avoir une photo à l'instant T sans observer en continu ici
                val localDecisions = swipeDecisionRepository.getDecisionsForAlbum(album.id).first()
                
                // Durée d'expiration en millisecondes
                val lifespanDays = sessionRepository.skipLifespanDays.first()
                val lifespanMs = lifespanDays * 24 * 60 * 60 * 1000L
                val currentTime = System.currentTimeMillis()

                // On nettoie réellement la base de données pour les SKIP expirés
                if (lifespanDays > 0) {
                    swipeDecisionRepository.cleanExpiredSkips(lifespanDays)
                }

                // On transforme la liste de SwipeDecisionEntity en Map<String, SwipeDecision>
                // On filtre les SKIP expirés (au cas où le cleanup prend du temps)
                val decisionMap = localDecisions.filter { entity ->
                    val decision = SwipeDecision.valueOf(entity.decision)
                    if (decision == SwipeDecision.SKIP && lifespanDays > 0) {
                        val isExpired = (currentTime - entity.createdAt) > lifespanMs
                        !isExpired
                    } else {
                        true
                    }
                }.associate { entity ->
                    entity.assetId to SwipeDecision.valueOf(entity.decision)
                }.toMutableMap()

                // NETTOYAGE : Si on a des décisions locales pour des assets qui n'existent plus
                // dans cet album sur le serveur, on les supprime.
                // Cela évite les compteurs incohérents (ex: 7/6 triés).
                val serverAssetIds = assets.map { it.id }.toSet()
                val invalidAssetIds = localDecisions.map { it.assetId }.filter { !serverAssetIds.contains(it) }
                
                if (invalidAssetIds.isNotEmpty()) {
                    // Pour le nettoyage au chargement, on est prudent : on ne nettoie que pour CET album
                    // car l'absence dans CET album ne veut pas dire que l'asset est mort sur Immich
                    // (il a pu être simplement retiré de l'album).
                    swipeDecisionRepository.removeDecisions(invalidAssetIds, album.id)
                    invalidAssetIds.forEach { decisionMap.remove(it) }
                }

                // On cherche le premier index non traité
                val firstUnprocessedIndex = assets.indexOfFirst { !decisionMap.containsKey(it.id) }
                    .let { if (it == -1) assets.size else it }

                _uiState.value = _uiState.value.copy(
                    assets = assets,
                    decisions = decisionMap,
                    currentIndex = firstUnprocessedIndex,
                    isLoading = false
                )
                
                // On charge les détails de l'asset actuel
                if (firstUnprocessedIndex < assets.size) {
                    loadAssetDetail(assets[firstUnprocessedIndex].id, firstUnprocessedIndex)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erreur lors du chargement des photos"
                )
            }
        }
    }

    private fun loadAssetDetail(assetId: String, index: Int) {
        viewModelScope.launch {
            try {
                val detail = assetRepository.getAssetDetail(assetId)
                val currentAssets = _uiState.value.assets.toMutableList()
                if (index < currentAssets.size) {
                    currentAssets[index] = detail
                    _uiState.value = _uiState.value.copy(assets = currentAssets)
                }
            } catch (e: Exception) {
                // Erreur silencieuse pour les détails
                android.util.Log.e("SWIPE_VM", "Erreur details asset: ${e.message}")
            }
        }
    }

    fun onSwipe(decision: SwipeDecision) {
        val currentState = _uiState.value
        val currentAsset = currentState.currentAsset ?: return

        // 1. Sauvegarde en base locale (Room)
        viewModelScope.launch {
            swipeDecisionRepository.saveDecision(
                assetId = currentAsset.id,
                albumId = album.id,
                decision = decision.name
            )
        }

        // 2. Mise à jour de l'UI
        val newDecisions = currentState.decisions.toMutableMap()
        newDecisions[currentAsset.id] = decision

        val newHistory = currentState.history.toMutableList()
        newHistory.add(currentAsset.id)

        // Trouver le prochain asset à afficher
        val assets = currentState.assets
        var nextIndex = -1
        
        // 1. Chercher d'abord la prochaine photo NON TRAITÉE après l'actuelle
        for (i in (currentState.currentIndex + 1) until assets.size) {
            if (!newDecisions.containsKey(assets[i].id)) {
                nextIndex = i
                break
            }
        }
        
        // 2. Si rien trouvé après, chercher une photo NON TRAITÉE depuis le début (boucle)
        if (nextIndex == -1) {
            for (i in 0 until currentState.currentIndex) {
                if (!newDecisions.containsKey(assets[i].id)) {
                    nextIndex = i
                    break
                }
            }
        }
        
        // 3. Si TOUT est traité (Mode Revue), on passe simplement au suivant dans l'ordre de la liste
        if (nextIndex == -1) {
            if (currentState.currentIndex + 1 < assets.size) {
                nextIndex = currentState.currentIndex + 1
            } else {
                nextIndex = assets.size // Fin réelle de l'album
            }
        }

        _uiState.value = currentState.copy(
            currentIndex = nextIndex,
            decisions = newDecisions,
            history = newHistory
        )

        // Anticipation : charge les détails du prochain
        if (nextIndex < assets.size) {
            loadAssetDetail(assets[nextIndex].id, nextIndex)
        }
    }

    fun undo() {
        val currentState = _uiState.value
        val lastAssetIdFromHistory = currentState.history.lastOrNull()

        viewModelScope.launch {
            if (lastAssetIdFromHistory != null) {
                // 1. LOGIQUE DE SESSION (Historique présent)
                swipeDecisionRepository.removeDecision(lastAssetIdFromHistory, album.id)
                
                val newDecisions = currentState.decisions.toMutableMap()
                newDecisions.remove(lastAssetIdFromHistory)

                val newHistory = currentState.history.toMutableList()
                newHistory.removeAt(newHistory.size - 1)

                val previousIndex = currentState.assets.indexOfFirst { it.id == lastAssetIdFromHistory }

                _uiState.value = currentState.copy(
                    currentIndex = if (previousIndex != -1) previousIndex else currentState.currentIndex,
                    decisions = newDecisions,
                    history = newHistory
                )
                
                if (previousIndex != -1) {
                    loadAssetDetail(lastAssetIdFromHistory, previousIndex)
                }
            } else if (currentState.currentIndex > 0) {
                // 2. LOGIQUE DE REMONTÉE (Historique vide, on recule manuellement)
                // "annule l'asset affiché actuellement, puis passe au précédent"
                val currentAsset = currentState.currentAsset
                val previousIndex = currentState.currentIndex - 1
                val previousAssetId = currentState.assets[previousIndex].id

                // On nettoie UNIQUEMENT l'actuel
                if (currentAsset != null) {
                    swipeDecisionRepository.removeDecision(currentAsset.id, album.id)
                }
                
                val newDecisions = currentState.decisions.toMutableMap()
                if (currentAsset != null) {
                    newDecisions.remove(currentAsset.id)
                }

                _uiState.value = currentState.copy(
                    currentIndex = previousIndex,
                    decisions = newDecisions
                )
                
                loadAssetDetail(previousAssetId, previousIndex)
            }
        }
    }

    /**
     * Permet de sauter directement à un asset précis (via la timeline).
     */
    fun onMoveToAsset(index: Int) {
        if (index in 0 until _uiState.value.assets.size) {
            _uiState.value = _uiState.value.copy(currentIndex = index)
            loadAssetDetail(_uiState.value.assets[index].id, index)
        }
    }

    /**
     * Affiche ou cache l'écran de résumé.
     */
    fun toggleSummary(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showSummary = visible)
    }

    /**
     * Annule une décision spécifique (utilisé depuis le résumé).
     */
    fun undoSpecificDecision(assetId: String) {
        val currentState = _uiState.value
        viewModelScope.launch {
            // 1. Suppression base Room
            swipeDecisionRepository.removeDecision(assetId, album.id)
            
            // 2. Mise à jour UI
            val newDecisions = currentState.decisions.toMutableMap()
            newDecisions.remove(assetId)
            
            val newHistory = currentState.history.toMutableList()
            newHistory.remove(assetId)
            
            _uiState.value = currentState.copy(
                decisions = newDecisions,
                history = newHistory
            )
        }
    }

    /**
     * Applique les décisions (Suppression sur Immich) et marque les assets comme traités localement.
     */
    fun applyChanges() {
        val currentState = _uiState.value
        val toDelete = currentState.decisions.filter { it.value == SwipeDecision.DELETE }.keys.toList()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            try {
                // 1. Appel API pour supprimer sur Immich
                if (toDelete.isNotEmpty()) {
                    assetRepository.deleteAssets(toDelete)
                }

                // 2. Vérification immédiate : On recharge la liste depuis le serveur
                val freshAssets = assetRepository.getAssetsByAlbum(album.id)
                val freshIds = freshAssets.map { it.id }.toSet()

                // 3. Identification des échecs : ceux qu'on a voulu supprimer mais qui sont encore là
                val failedDeletions = toDelete.filter { freshIds.contains(it) }
                
                // 4. Nettoyage de la base locale :
                // On supprime de Room les décisions pour les assets qui ne sont PLUS sur le serveur.
                // IMPORTANT : Si une photo a été supprimée, on la retire de TOUS les albums pour éviter
                // les incohérences de compteurs (ex: 7/6 assets triés).
                val disappearedIds = currentState.decisions.keys.filter { !freshIds.contains(it) }
                if (disappearedIds.isNotEmpty()) {
                    swipeDecisionRepository.removeDecisionsFromAllAlbums(disappearedIds)
                }

                if (failedDeletions.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        showSummary = false,
                        error = "Attention : ${failedDeletions.size} photos n'ont pas pu être supprimées. Vérifiez votre connexion ou vos droits."
                    )
                } else {
                    // Succès total : On lance l'animation
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        showSummary = false,
                        showSuccessAnimation = true
                    )
                    // On cache l'animation après 2.5 secondes
                    delay(2500)
                    _uiState.value = _uiState.value.copy(showSuccessAnimation = false)
                }
                
                // On recharge tout l'état proprement (merge final)
                loadAssetsAndDecisions()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    error = "Erreur lors de la synchronisation : ${e.message}"
                )
            }
        }
    }

    /**
     * Calcule l'index du prochain asset à afficher en arrière-plan.
     * Priorité aux non-traités, sinon le suivant dans la liste.
     */
    fun getNextUnprocessedIndex(): Int {
        val state = _uiState.value
        val assets = state.assets
        val decisions = state.decisions
        val current = state.currentIndex

        // 1. Chercher le prochain non-traité après
        for (i in (current + 1) until assets.size) {
            if (!decisions.containsKey(assets[i].id)) return i
        }
        
        // 2. Chercher le prochain non-traité avant
        for (i in 0 until current) {
            if (!decisions.containsKey(assets[i].id)) return i
        }

        // 3. Si tout est traité, on affiche simplement la carte suivante dans la liste
        if (current + 1 < assets.size) {
            return current + 1
        }
        
        return -1
    }
}
