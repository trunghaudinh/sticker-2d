package com.example.sticker2d.face_detection

import android.graphics.*
import com.example.sticker2d.camerax.GraphicOverlay
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour

class FaceContourGraphic(
    overlay: GraphicOverlay,
    private val face: Face,
    private val imageRect: Rect
) : GraphicOverlay.Graphic(overlay) {

    private val facePositionPaint: Paint
    private val eyePositionPaint: Paint
    private val idPaint: Paint
    private val boxPaint: Paint
    var path : Path? = null
    private var linePaint : Paint? = null
    private var dotPaint : Paint? = null
    init {
        val selectedColor = Color.WHITE

        facePositionPaint = Paint()
        facePositionPaint.color = selectedColor

        // eye
        eyePositionPaint = Paint().apply {
            color = Color.RED
        }

        idPaint = Paint()
        idPaint.color = selectedColor

        boxPaint = Paint()
        boxPaint.color = selectedColor
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = BOX_STROKE_WIDTH

         dotPaint = Paint().apply {
             color = Color.RED
             style = Paint.Style.FILL
             strokeWidth = 4F
         }

         linePaint = Paint().apply {
             color = Color.GREEN
             style = Paint.Style.STROKE
             strokeWidth = 2F
         }

    }



    override fun draw(canvas: Canvas?) {
        val rect = calculateRect(
            imageRect.height().toFloat(),
            imageRect.width().toFloat(),
            face.boundingBox
        )
        canvas?.drawRect(rect, boxPaint)

        val faceContours = face.getContour(FaceContour.FACE)!!.points
        for (i in 0 until faceContours.size){
            if (i != faceContours.lastIndex){
                canvas?.drawLine(faceContours[i].x, faceContours[i].y, faceContours[i + 1].x, faceContours[i + 1].y, linePaint!!)
            }else{
                canvas?.drawLine(faceContours[i].x, faceContours[i].y, faceContours[0].x, faceContours[0].y, linePaint!!)
            }
            canvas?.drawCircle(faceContours[i].x, faceContours[i].y, 4F, dotPaint!!)
        }
    }

    companion object {
        private const val BOX_STROKE_WIDTH = 5.0f
    }

}