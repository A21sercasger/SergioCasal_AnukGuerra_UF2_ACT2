package com.example.sergiocasal_anukguerra_uf2_act2

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.AttributeSet
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat

class PianoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Constantes
    companion object {
        private const val NUM_KEYS = 25 // C2 a C4 (2 octavas + 1)
        private const val NOTES_PER_OCTAVE = 12
        private const val BLACK_KEY_WIDTH_RATIO = 0.6f
        private const val BLACK_KEY_HEIGHT_RATIO = 0.6f
    }

    // Arrays para controlar las teclas y las notas
    private lateinit var whiteKeyRects: Array<RectF>
    private lateinit var blackKeyRects: Array<RectF>
    private lateinit var whiteKeyIndices: IntArray
    private lateinit var blackKeyIndices: IntArray
    private lateinit var whiteKeyPressed: BooleanArray
    private lateinit var blackKeyPressed: BooleanArray

    // Map para seguir qué puntero (pointer) está presionando qué tecla
    private val pointerWhiteKeyMap = HashMap<Int, Int>()
    private val pointerBlackKeyMap = HashMap<Int, Int>()

    // Drawables para las teclas
    private val whiteKeyDrawable: Drawable?
    private val blackKeyDrawable: Drawable?

    // SoundPool para reproducir sonidos
    private var soundPool: SoundPool
    private val soundMap = SparseIntArray()
    private val streamIdMap = SparseArray<Int>()

    init {
        // Inicializar los drawables
        whiteKeyDrawable = ContextCompat.getDrawable(context, R.drawable.white_key)
        blackKeyDrawable = ContextCompat.getDrawable(context, R.drawable.black_key)

        // Contar las teclas blancas y negras
        var whiteKeyCount = 0
        var blackKeyCount = 0
        for (i in 0 until NUM_KEYS) {
            val note = i % NOTES_PER_OCTAVE
            if (note == 1 || note == 3 || note == 6 || note == 8 || note == 10) {
                blackKeyCount++
            } else {
                whiteKeyCount++
            }
        }

        // Inicializar los arrays para las teclas
        whiteKeyRects = Array(whiteKeyCount) { RectF() }
        blackKeyRects = Array(blackKeyCount) { RectF() }
        whiteKeyIndices = IntArray(whiteKeyCount)
        blackKeyIndices = IntArray(blackKeyCount)
        whiteKeyPressed = BooleanArray(whiteKeyCount) { false }
        blackKeyPressed = BooleanArray(blackKeyCount) { false }

        // Configurar las teclas y sus índices
        var whiteKeyIndex = 0
        var blackKeyIndex = 0
        for (i in 0 until NUM_KEYS) {
            val note = i % NOTES_PER_OCTAVE
            if (note == 1 || note == 3 || note == 6 || note == 8 || note == 10) {
                // Tecla negra
                blackKeyIndices[blackKeyIndex] = i
                blackKeyIndex++
            } else {
                // Tecla blanca
                whiteKeyIndices[whiteKeyIndex] = i
                whiteKeyIndex++
            }
        }

        // Inicializar SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(NUM_KEYS)
            .setAudioAttributes(audioAttributes)
            .build()

        // Cargar sonidos (estos deberían estar en la carpeta res/raw)
        // Aquí debes cargar los sonidos para cada nota desde C2 hasta C4
        for (i in 0 until NUM_KEYS) {
            val resId = context.resources.getIdentifier(
                "note_$i", "raw", context.packageName
            )
            if (resId != 0) {
                soundMap.put(i, soundPool.load(context, resId, 1))
            }
        }

        // Habilitar el feedback táctil
        isClickable = true
        isFocusable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Calcular las dimensiones de las teclas blancas
        val whiteKeyWidth = w / whiteKeyRects.size.toFloat()
        val whiteKeyHeight = h.toFloat()

        // Actualizar los rectángulos para las teclas blancas
        for (i in whiteKeyRects.indices) {
            whiteKeyRects[i].set(i * whiteKeyWidth, 0f, (i + 1) * whiteKeyWidth, whiteKeyHeight)
        }

        // Actualizar los rectángulos para las teclas negras
        val blackKeyWidth = whiteKeyWidth * BLACK_KEY_WIDTH_RATIO
        val blackKeyHeight = whiteKeyHeight * BLACK_KEY_HEIGHT_RATIO

        var blackKeyIndex = 0
        for (i in 0 until whiteKeyRects.size - 1) {
            val note = whiteKeyIndices[i] % NOTES_PER_OCTAVE
            if (note == 0 || note == 2 || note == 5 || note == 7 || note == 9) {
                // Después de C, D, F, G, A viene una tecla negra
                val x = whiteKeyRects[i].right - blackKeyWidth / 2
                blackKeyRects[blackKeyIndex].set(x, 0f, x + blackKeyWidth, blackKeyHeight)
                blackKeyIndex++
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Dibujar las teclas blancas
        for (i in whiteKeyRects.indices) {
            whiteKeyDrawable?.let {
                it.state = if (whiteKeyPressed[i]) intArrayOf(android.R.attr.state_pressed) else intArrayOf()
                it.setBounds(
                    whiteKeyRects[i].left.toInt(),
                    whiteKeyRects[i].top.toInt(),
                    whiteKeyRects[i].right.toInt(),
                    whiteKeyRects[i].bottom.toInt()
                )
                it.draw(canvas)
            }
        }

        // Dibujar las teclas negras
        for (i in blackKeyRects.indices) {
            blackKeyDrawable?.let {
                it.state = if (blackKeyPressed[i]) intArrayOf(android.R.attr.state_pressed) else intArrayOf()
                it.setBounds(
                    blackKeyRects[i].left.toInt(),
                    blackKeyRects[i].top.toInt(),
                    blackKeyRects[i].right.toInt(),
                    blackKeyRects[i].bottom.toInt()
                )
                it.draw(canvas)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // Comprobar si se tocó una tecla negra primero (están encima)
                var blackKeyTouched = false
                for (i in blackKeyRects.indices) {
                    if (blackKeyRects[i].contains(x, y)) {
                        pressBlackKey(i, pointerId)
                        blackKeyTouched = true
                        performClick()
                        break
                    }
                }

                // Si no se tocó una tecla negra, comprobar las teclas blancas
                if (!blackKeyTouched) {
                    for (i in whiteKeyRects.indices) {
                        if (whiteKeyRects[i].contains(x, y)) {
                            pressWhiteKey(i, pointerId)
                            performClick()
                            break
                        }
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // Manejar el movimiento de dedos en la pantalla
                for (i in 0 until event.pointerCount) {
                    val pId = event.getPointerId(i)
                    val pX = event.getX(i)
                    val pY = event.getY(i)

                    // Verificar si este puntero está presionando una tecla blanca
                    pointerWhiteKeyMap[pId]?.let { whiteKeyIndex ->
                        // Si el dedo se ha movido fuera de la tecla, soltarla
                        if (!whiteKeyRects[whiteKeyIndex].contains(pX, pY)) {
                            releaseWhiteKey(whiteKeyIndex, pId)

                            // Comprobar si ahora está sobre otra tecla
                            var foundNewKey = false

                            // Primero comprobar teclas negras
                            for (j in blackKeyRects.indices) {
                                if (blackKeyRects[j].contains(pX, pY)) {
                                    pressBlackKey(j, pId)
                                    foundNewKey = true
                                    break
                                }
                            }

                            // Si no está sobre una tecla negra, comprobar teclas blancas
                            if (!foundNewKey) {
                                for (j in whiteKeyRects.indices) {
                                    if (whiteKeyRects[j].contains(pX, pY)) {
                                        pressWhiteKey(j, pId)
                                        break
                                    }
                                }
                            }
                        }
                    }

                    // Verificar si este puntero está presionando una tecla negra
                    pointerBlackKeyMap[pId]?.let { blackKeyIndex ->
                        // Si el dedo se ha movido fuera de la tecla, soltarla
                        if (!blackKeyRects[blackKeyIndex].contains(pX, pY)) {
                            releaseBlackKey(blackKeyIndex, pId)

                            // Comprobar si ahora está sobre otra tecla
                            var foundNewKey = false

                            // Primero comprobar teclas negras
                            for (j in blackKeyRects.indices) {
                                if (blackKeyRects[j].contains(pX, pY)) {
                                    pressBlackKey(j, pId)
                                    foundNewKey = true
                                    break
                                }
                            }

                            // Si no está sobre una tecla negra, comprobar teclas blancas
                            if (!foundNewKey) {
                                for (j in whiteKeyRects.indices) {
                                    if (whiteKeyRects[j].contains(pX, pY)) {
                                        pressWhiteKey(j, pId)
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                // Soltar la tecla correspondiente al puntero
                pointerWhiteKeyMap[pointerId]?.let { whiteKey ->
                    releaseWhiteKey(whiteKey, pointerId)
                }

                pointerBlackKeyMap[pointerId]?.let { blackKey ->
                    releaseBlackKey(blackKey, pointerId)
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                // Liberar todas las teclas
                for (i in whiteKeyPressed.indices) {
                    whiteKeyPressed[i] = false
                }
                pointerWhiteKeyMap.clear()

                for (i in blackKeyPressed.indices) {
                    blackKeyPressed[i] = false
                }
                pointerBlackKeyMap.clear()

                stopAllSounds()
                invalidate()
            }
        }

        return true
    }

    private fun pressWhiteKey(index: Int, pointerId: Int) {
        if (!whiteKeyPressed[index]) {
            whiteKeyPressed[index] = true
            pointerWhiteKeyMap[pointerId] = index

            // Reproducir sonido
            val noteIndex = whiteKeyIndices[index]
            playSound(noteIndex)

            invalidate()
        }
    }

    private fun releaseWhiteKey(index: Int, pointerId: Int) {
        if (whiteKeyPressed[index]) {
            whiteKeyPressed[index] = false
            pointerWhiteKeyMap.remove(pointerId)

            // Detener sonido
            val noteIndex = whiteKeyIndices[index]
            stopSound(noteIndex)

            invalidate()
        }
    }

    private fun pressBlackKey(index: Int, pointerId: Int) {
        if (!blackKeyPressed[index]) {
            blackKeyPressed[index] = true
            pointerBlackKeyMap[pointerId] = index

            // Reproducir sonido
            val noteIndex = blackKeyIndices[index]
            playSound(noteIndex)

            invalidate()
        }
    }

    private fun releaseBlackKey(index: Int, pointerId: Int) {
        if (blackKeyPressed[index]) {
            blackKeyPressed[index] = false
            pointerBlackKeyMap.remove(pointerId)

            // Detener sonido
            val noteIndex = blackKeyIndices[index]
            stopSound(noteIndex)

            invalidate()
        }
    }

    private fun playSound(noteIndex: Int) {
        val soundId = soundMap.get(noteIndex, -1)
        if (soundId != -1) {
            val streamId = soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
            streamIdMap.put(noteIndex, streamId)
        }
    }

    private fun stopSound(noteIndex: Int) {
        streamIdMap.get(noteIndex)?.let { streamId ->
            soundPool.stop(streamId)
            streamIdMap.remove(noteIndex)
        }
    }

    private fun stopAllSounds() {
        for (i in 0 until streamIdMap.size()) {
            val streamId = streamIdMap.valueAt(i)
            soundPool.stop(streamId)
        }
        streamIdMap.clear()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        soundPool.release()
    }
}