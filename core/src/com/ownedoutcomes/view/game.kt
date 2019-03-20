package com.ownedoutcomes.view

import box2dLight.ConeLight
import box2dLight.PointLight
import box2dLight.RayHandler
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.kotcrab.vis.ui.widget.Draggable
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisLabel
import com.ownedoutcomes.Main
import com.ownedoutcomes.music.Prompt
import com.ownedoutcomes.music.randomCaughtPrompt
import com.ownedoutcomes.music.randomMissmatchPrompt
import com.ownedoutcomes.music.randomRunPrompt
import com.ownedoutcomes.view.entity.*
import ktx.actors.alpha
import ktx.actors.onChange
import ktx.actors.onClick
import ktx.actors.then
import ktx.assets.asset
import ktx.assets.getValue
import ktx.assets.loadOnDemand
import ktx.collections.gdxArrayOf
import ktx.inject.inject
import ktx.math.vec2
import ktx.vis.KFloatingGroup
import ktx.vis.floatingGroup
import ktx.vis.table
import ktx.vis.window
import java.util.*

class Game(stage: Stage, skin: Skin, val batch: Batch) : AbstractView(stage) {
    val backgroundImage by loadOnDemand<Texture>(path = "background.png")
    val rayHandler = RayHandler(World(vec2(), true))
    val light = PointLight(rayHandler, 30, Color(0f, 0f, 0f, 1f), initialLightDistance, 100f, 100f)
    val lightPosition = vec2()
    val dragController = DragController(this, stage)
    val spawner = Spawner(stage, dragController, this)
    val handImage = VisImage("hand")
    val promptLabel = VisLabel("", "title")
    val healthIcons = gdxArrayOf<Image>()
    override val root: Actor
    lateinit var urineImage: Image
    lateinit var beerImage: Image
    lateinit var vodkaImage: Image
    lateinit var smokeImage: Image
    lateinit var pointsLabel: Label
    private var firstShow = true

    init {
        light.isXray = true
        ConeLight(rayHandler, 15, Color.BLACK, 600f, 600f, 400f, 45f, 45f)  // Bonuses.
        ConeLight(rayHandler, 15, Color.BLACK, 350f, 300f, 540f, -45f, 45f) // Room light.
        root = table {
            backgroundImage.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            background = TextureRegionDrawable(TextureRegion(backgroundImage, 0, 0, 1000, 750))
            setFillParent(true)
            table {
                padLeft(140f)
                padRight(10f)
                pointsLabel = label("0", styleName = "title") {
                    color = Color.DARK_GRAY
                    setAlignment(Align.center)
                }.width(100f).padBottom(155f).padLeft(60f).actor
            }.align(Align.right).expand()
            table {
                healthIcons.add(image("health").actor)
                row()
                healthIcons.add(image("health").actor)
                row()
                healthIcons.add(image("health").expandY().align(Align.top).actor)
            }.expandY().align(Align.top).padTop(50f)
            table {
                urineImage = image(drawableName = "piss").actor
                image(drawableName = "beer") {
                    beerImage = this
                }.row()
                vodkaImage = image(drawableName = "vodka").actor
                smokeImage = image(drawableName = "smoke").actor
            }.expand().align(Align.topRight)
        }
        handImage.setPosition(312f, 387f)
        handImage.setOrigin(10f, 70f)
        resetPrompt()
    }

    fun resetPrompt() {
        promptLabel.clearActions()
        promptLabel.setPosition(380f, 510f)
        promptLabel.alpha = 0f
    }

    val actorsComparator = Comparator<Actor> { o1, o2 ->
        when {
            o1 === o2 -> 0
            o1 === root -> -1
            o2 === root -> 1
            o1 === handImage -> -1
            o2 === handImage -> 1
            o1 is Draggable.MimicActor -> 1
            o2 is Draggable.MimicActor -> 1
            o1 is Label -> 1
            o2 is Label -> 1
            o1 is Urine && o2 is Urine -> 0
            o1 is Urine -> -1
            o2 is Urine -> 1
            o1.y < o2.y -> 1
            o1.y == o2.y -> 0
            else -> -1
        }
    }

    override fun render(delta: Float) {
        sortActors()
        super.render(delta)
        if (firstShow) {
            return
        }
        updateHand()
        spawner.update(delta)
        stage.screenToStageCoordinates(lightPosition.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()));
        light.position = lightPosition
        light.update()
        rayHandler.setCombinedMatrix(stage.camera.combined)
        rayHandler.updateAndRender()
    }

    private fun updateHand() {
        val angle = MathUtils.atan2(lightPosition.y - handImage.y, lightPosition.x - handImage.x)
        handImage.rotation = MathUtils.radiansToDegrees * angle
    }

    override fun show() {
        if (firstShow) {
            showTutorial()
        }
        super.show()
        light.distance = initialLightDistance
        rayHandler.setAmbientLight(0f, 0f, 0f, 0.1f)
        stage.addActor(handImage)
        spawner.reset()
        dragController.reset()
        stage.addActor(promptLabel)
        healthIcons.forEach { it.alpha = 1f }
    }

    private fun showTutorial() {
        val tutorial = floatingGroup {
            table {
                this.touchable = Touchable.enabled
                width = 800f
                height = 600f
            }
            image("beer2") {
                x = 850f
                y = 75f
                setOrigin(width / 2f, height / 2f)
                addAction(Actions.forever(Actions.sequence(
                        Actions.rotateBy(20f, 0.2f),
                        Actions.rotateBy(-40f, 0.4f),
                        Actions.rotateBy(20f, 0.2f)
                )))
                addAction(Actions.forever(Actions.moveTo(630f, 75f, 2f)
                        then Actions.moveTo(690f, 510f, 1f)
                        then Actions.fadeOut(0.2f)
                        then Actions.moveTo(850f, 75f)
                        then Actions.alpha(1f)))
            }
            image("tutorialcursor") {
                x = 850f
                y = 300f
                setOrigin(width / 2f, height / 2f)
                addAction(Actions.forever(Actions.delay(1.4f)
                        then Actions.moveTo(630f, 75f, 0.6f)
                        then Actions.moveTo(690f, 510f, 1f)
                        then Actions.moveTo(850f, 300f, 0.22f)))
            }
            addTutorialLabel("To Ty: służebnica\nprawa i obyczajów. >", x = 60f, y = 460f)
            addTutorialLabel("Tylu degeneratów >\nzostało ukaranych.", x = 70f, y = 300f)
            addTutorialLabel("< Tędy chadzają ludzie. >", x = 250f, y = 120f)
            addTutorialLabel("Uważaj na prawych obywateli, a karaj nicponi.", x = 120f, y = 60f)
            addTutorialLabel("Teraz kliknij gdziekolwiek, by zacząć!", x = 160f, y = 10f)
            addTutorialLabel("                         >\nTutaj zgłaszamy złoczyńców,\nprzeciągając ich\nna ikony zgodne\nz przewinieniami.",
                    x = 410f, y = 210f)
            touchable = Touchable.enabled
            onClick { event, group ->
                firstShow = false
                this.remove()
            }
        }
        stage.addActor(tutorial)
    }

    private fun KFloatingGroup.addTutorialLabel(text: String, x: Float, y: Float) {
        this.label(text, styleName = "title") {
            color = Color.BLACK
            this.y = y - 2f
            this.x = x + 2f
        }
        this.label(text, styleName = "title") {
            color = Color.CORAL
            this.y = y
            this.x = x
        }
    }

    private fun sortActors() {
        stage.actors.sort(actorsComparator)
    }

    fun lose(points: Int) {
        rayHandler.setAmbientLight(0f, 0f, 0f, 1f)
        showLoseDialog(points)
    }

    private fun showLoseDialog(points: Int) {
        val dialog = window(title = if (points > 0) "Dziękujemy!" else "Peszek!", styleName = "dialog") {
            titleLabel.setEllipsis(false)
            titleTable.getCell(titleLabel).padLeft(75f).padRight(100f).expandX().align(Align.center)
            padTop(50f)
            defaults().pad(5f)
            isModal = true
            isMovable = false
            if (points > 0) {
                table {
                    val pointsAsString = points.toString()
                    label(pointsAsString, styleName = "title") {
                        color = Color.FIREBRICK
                    }.padRight(8f)
                    val lastDigit = Integer.parseInt(pointsAsString[pointsAsString.length - 1].toString())
                    val polishLol = when (lastDigit) {
                        1 -> "hultaj ma"
                        in 2..4 -> "hultaje mają"
                        else -> "hultajów ma"
                    }
                    label("$polishLol przekichane.", styleName = "title") {
                        color = Color.GRAY
                    }
                }.row()
            }
            textButton("Ja chcę jeszcze raz!", styleName = "title") {
                onChange { event, button ->
                    inject<Main>().setCurrentView(this@Game)
                }
            }.row()
            textButton("Mam dość...", styleName = "title") {
                onChange { event, button ->
                    inject<Main>().setCurrentView(inject<Menu>())
                }
            }
        }
        stage.addActor(dialog)
        dialog.pack()
        dialog.centerWindow()
        dialog.fadeIn()
    }

    fun improveVision() {
        light.distance += 10f
    }
}

val initialLightDistance = 150f

class Spawner(val stage: Stage, val dragController: DragController, val game: Game) {
    var timeToSpawn = 0f
    var timeToRandomEvent = 0f

    fun update(delta: Float) {
        if (dragController.lives <= 0) {
            return
        }
        timeToSpawn -= delta
        if (timeToSpawn <= 0f) {
            spawn()
            timeToSpawn = getNextTimeToSpawn()
            println("Next time to spawn: $timeToSpawn")
        }
        timeToRandomEvent -= delta
        if (timeToRandomEvent <= 0f) {
            randomEvent()
            timeToRandomEvent = MathUtils.random(8f, 12f)
            println("Next time to spawn random: $timeToRandomEvent")
        }
    }

    private fun getNextTimeToSpawn(): Float {
        val base = MathUtils.random(1.5f, 3f)
        val points = dragController.points
        return when (points) {
            in 0..50 -> base + 1.5f - 1.5f * (points / 50f)
            in 51..75 -> base * 0.9f
            in 75..100 -> base * 0.8f
            else -> base * 0.7f
        }
    }

    private fun randomEvent() {
        when (getRandomRandomEvent()) {
            RandomEvent.VODKA -> spawnVodka()
            RandomEvent.GRANNY -> spawnGranny()
            RandomEvent.GLASSES -> spawnGlasses()
            RandomEvent.BATTERY -> spawnBattery()
        }
    }

    private fun spawn() {
        stage.addActor(getRandomPedestrian(dragController))
    }

    private fun spawnVodka() {
        stage.addActor(Vodka(game))
    }

    private fun spawnBattery() {
        stage.addActor(Battery(game))
    }

    private fun spawnGlasses() {
        stage.addActor(Glasses(game))
    }

    private fun spawnGranny() {
        stage.addActor(Granny(dragController))
    }

    fun reset() {
        timeToSpawn = 0f
        timeToRandomEvent = 5f
    }
}

class DragController(val game: Game, val stage: Stage) {
    var currentSound: Sound? = null
    var lives = 3
    var points = 0

    fun reset() {
        lives = 3
        points = 0
        game.pointsLabel.setText("0")
    }

    fun getDraggable(): Draggable {
        val draggable = Draggable()
        draggable.listener = object : Draggable.DragAdapter() {
            override fun onEnd(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean {
                val hit = stage.hit(stageX, stageY, false)
                if (hit == null) {
                    return Draggable.DragListener.APPROVE
                }
                val pedestrian = actor as Pedestrian
                when (hit) {
                    game.urineImage -> checkPedestrian(pedestrian, PedestrianType.URINE)
                    game.beerImage -> checkPedestrian(pedestrian, PedestrianType.BEER)
                    game.smokeImage -> checkPedestrian(pedestrian, PedestrianType.SMOKE)
                    game.vodkaImage -> checkPedestrian(pedestrian, PedestrianType.VODKA)
                }
                return Draggable.DragListener.APPROVE
            }
        }
        return draggable
    }

    fun checkPedestrian(pedestrian: Pedestrian, pedestrianType: PedestrianType) {
        if (pedestrian.type == pedestrianType) {
            pedestrian.clearActions()
            pedestrian.removeListener(pedestrian.listeners.first())
            pedestrian.addAction(Actions.sequence(Actions.fadeOut(0.2f), Actions.removeActor()))
            incrementPoints()
        } else {
            decrementPoints()
        }
    }

    fun incrementPoints() {
        lives++
        if (lives > 3) {
            lives = 3
        }
        updateLiveIcons()
        points++
        updatePointsLabel()
        displayPrompt(randomCaughtPrompt(), negative = false)
    }

    fun boostPoints(prompt: Prompt) {
        points += 2
        updatePointsLabel()
        displayPrompt(prompt, negative = false)
    }

    private fun updatePointsLabel() {
        game.pointsLabel.setText(points.toString())
        game.pointsLabel.addAction(Actions.sequence(
                Actions.moveBy(0f, 5f, 0.1f),
                Actions.moveBy(0f, -5f, 0.1f)
        ))
    }

    private fun updateLiveIcons() {
        checkIcon(0)
        checkIcon(1)
        checkIcon(2)
    }

    private fun checkIcon(id: Int) {
        if (lives <= id) {
            game.healthIcons[id].addAction(Actions.alpha(0.4f, 0.1f))
        } else {
            game.healthIcons[id].addAction(Actions.alpha(1f, 0.1f))
        }
    }

    fun displayPrompt(prompt: Prompt, negative: Boolean = true) {
        game.resetPrompt()
        if (negative) {
            game.promptLabel.color = negativeColor
        } else {
            game.promptLabel.color = positiveColor
        }
        game.promptLabel.setText(prompt.prompt)
        currentSound?.stop()
        currentSound = asset<Sound>("sounds/${prompt.toString()}.wav").apply {
            play(0.5f)
        }
        game.promptLabel.addAction(Actions.parallel(
                Actions.fadeIn(0.1f),
                Actions.moveBy(15f, 10f, 0.15f)
        ) then Actions.delay(0.15f) then Actions.parallel(
                Actions.fadeOut(0.15f)
        ))
    }

    fun decrementPoints(run: Boolean = false) {
        lives--
        updateLiveIcons()
        if (lives == 0) {
            game.lose(points)
        } else if (lives > 0) {
            val prompt: Prompt = if (run) {
                randomRunPrompt()
            } else {
                randomMissmatchPrompt()
            }
            displayPrompt(prompt)
        }
    }
}

val positiveColor = Color(0.6f, 1f, 0.6f, 1f)
val negativeColor = Color(1f, 0.6f, 0.6f, 1f)