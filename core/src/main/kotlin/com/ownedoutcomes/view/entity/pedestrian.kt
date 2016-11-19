package com.ownedoutcomes.view.entity

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.widget.Draggable
import com.kotcrab.vis.ui.widget.VisImage
import com.ownedoutcomes.view.DragController
import ktx.actors.then
import ktx.collections.gdxArrayOf

enum class PedestrianType {
    REGULAR,
    REGULAR_AS_WELL,
    URINE,
    VODKA,
    BEER,
    SMOKE
}

fun getRandomPedestrian(dragController: DragController): Pedestrian {
    return when (PedestrianType.values()[MathUtils.random(0, PedestrianType.values().size - 1)]) {
        PedestrianType.REGULAR -> GoodCitizen(dragController)
        PedestrianType.REGULAR_AS_WELL -> GoodCitizen(dragController)
        PedestrianType.URINE -> Pisser(dragController)
        PedestrianType.BEER -> BeerDrinker(dragController)
        PedestrianType.VODKA -> VodkaDrinker(dragController)
        PedestrianType.SMOKE -> Smoker(dragController)
    }
}

abstract class Pedestrian(val drawableName: String, val dragController: DragController) : VisImage(drawableName) {
    private val sprite = Sprite(VisUI.getSkin().getRegion(drawableName))
    abstract val type: PedestrianType

    init {
        val targetX = setInitialPos()
        addListener(dragController.getDraggable())
        addInitialActions(targetX)
    }

    open protected fun setInitialPos(): Float {
        var x = -100f
        var targetX = 900f
        if (MathUtils.randomBoolean()) {
            x = targetX
            targetX = -100f
        } else {
            sprite.setFlip(true, false)
        }
        setPosition(x, randomY())
        return targetX
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        sprite.setPosition(x, y)
        sprite.rotation = rotation
        sprite.draw(batch)
    }

    protected fun randomY() = MathUtils.random(10f, 100f)

    open protected fun addInitialActions(targetX: Float) {
        setOrigin(width / 2f, height / 2f)
        addAction(Actions.forever(Actions.sequence(
                Actions.rotateBy(20f, 0.2f),
                Actions.rotateBy(-40f, 0.4f),
                Actions.rotateBy(20f, 0.2f)
        )))
        addAction(Actions.parallel(
                Actions.moveTo(targetX, randomY(), MathUtils.random(3f, 6f))
        ) then Actions.run {
            dragController.decrementPoints()
        } then Actions.removeActor())
    }
}

class GoodCitizen(dragController: DragController) : Pedestrian(gdxArrayOf("regular0", "regular1").random(), dragController) {
    override val type: PedestrianType
        get() = PedestrianType.REGULAR
}

class Pisser(dragController: DragController) : Pedestrian(gdxArrayOf("piss0").random(), dragController) {
    override val type: PedestrianType
        get() = PedestrianType.URINE

    // TODO inny action!
}

class BeerDrinker(dragController: DragController) : Pedestrian(gdxArrayOf("beer0").random(), dragController) {
    override val type: PedestrianType
        get() = PedestrianType.BEER
}

class VodkaDrinker(dragController: DragController) : Pedestrian(gdxArrayOf("vodka0").random(), dragController) {
    override val type: PedestrianType
        get() = PedestrianType.VODKA
}

class Smoker(dragController: DragController) : Pedestrian(gdxArrayOf("guy0").random(), dragController) {
    override val type: PedestrianType
        get() = PedestrianType.SMOKE
}