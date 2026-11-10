package com.hyper.castwave.utils

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener

@UnstableApi
class CastManager(private val context: Context) : SessionManagerListener<CastSession> {
    
    companion object {
        private const val TAG = "CastManager"
    }
    
    private var castContext: CastContext? = null
    var castPlayer: CastPlayer? = null
        private set

    @OptIn(UnstableApi::class)
    fun initialize() {
        try {
            castContext = CastContext.getSharedInstance(context)
            castPlayer = castContext?.let { CastPlayer(it) }
            
            castContext?.sessionManager?.addSessionManagerListener(this, CastSession::class.java)
            Log.d(TAG, "Cast Manager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Cast Manager: ${e.message}", e)
        }
    }
    
    fun isCastAvailable(): Boolean {
        return castContext != null
    }
    
    fun isCasting(): Boolean {
        return castContext?.sessionManager?.currentCastSession?.isConnected == true
    }
    
    override fun onSessionStarting(session: CastSession) {
        Log.d(TAG, "Cast session starting")
    }
    
    override fun onSessionStarted(session: CastSession, sessionId: String) {
        Log.d(TAG, "Cast session started: $sessionId")
    }
    
    override fun onSessionStartFailed(session: CastSession, error: Int) {
        Log.e(TAG, "Cast session start failed: $error")
    }
    
    override fun onSessionEnding(session: CastSession) {
        Log.d(TAG, "Cast session ending")
    }
    
    override fun onSessionEnded(session: CastSession, error: Int) {
        Log.d(TAG, "Cast session ended: $error")
    }
    
    override fun onSessionResuming(session: CastSession, sessionId: String) {
        Log.d(TAG, "Cast session resuming: $sessionId")
    }
    
    override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
        Log.d(TAG, "Cast session resumed")
    }
    
    override fun onSessionResumeFailed(session: CastSession, error: Int) {
        Log.e(TAG, "Cast session resume failed: $error")
    }
    
    override fun onSessionSuspended(session: CastSession, reason: Int) {
        Log.d(TAG, "Cast session suspended: $reason")
    }
    
    fun release() {
        castContext?.sessionManager?.removeSessionManagerListener(this, CastSession::class.java)
        castPlayer?.release()
        castPlayer = null
        castContext = null
    }
}


