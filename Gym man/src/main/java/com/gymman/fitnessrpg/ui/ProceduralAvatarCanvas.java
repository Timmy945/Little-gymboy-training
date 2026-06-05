package com.gymman.fitnessrpg.ui;

import com.gymman.fitnessrpg.model.MuscleGroup;
import com.gymman.fitnessrpg.visual.AvatarVisualState;
import com.gymman.fitnessrpg.visual.MaterialVisualState;
import com.gymman.fitnessrpg.visual.MuscleVisualState;
import com.gymman.fitnessrpg.visual.Scale3;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

final class ProceduralAvatarCanvas extends JPanel {
    private static final long TRANSITION_NANOS = 520_000_000L;
    private static final long FLASH_NANOS = 750_000_000L;
    private static final double LAYOUT_MARGIN = 6.0;
    private static final int MAX_TEXTURE_SIDE = 540;
    private static final boolean SHOW_DEBUG_OUTLINES = false;
    private static final double MAX_LIMB_LENGTH_GROWTH = 0.18;
    private static final double MAX_CORE_LENGTH_GROWTH = 0.24;

    private final List<BodyPrimitive> primitives = buildPrimitives();
    private final EnumMap<MuscleGroup, Long> flashStartNanos = new EnumMap<>(MuscleGroup.class);
    private final Timer animationTimer;

    private AvatarVisualState startState;
    private AvatarVisualState targetState;
    private long transitionStartNanos;
    private double yawRadians;
    private boolean autoRotate = true;
    private IntConsumer yawDegreesListener;

    ProceduralAvatarCanvas() {
        setBackground(new Color(17, 24, 39));
        setPreferredSize(new Dimension(700, 680));
        this.animationTimer = new Timer(16, event -> {
            if (autoRotate) {
                yawRadians = normalizeRadians(yawRadians + 0.0065);
                notifyYawChanged();
            }
            repaint();
        });
        animationTimer.start();
    }

    void setVisualState(AvatarVisualState visualState) {
        this.startState = targetState == null ? visualState : targetState;
        this.targetState = visualState;
        this.transitionStartNanos = System.nanoTime();
        repaint();
    }

    void setYawDegrees(int degrees) {
        this.yawRadians = normalizeRadians(Math.toRadians(degrees));
        notifyYawChanged();
        repaint();
    }

    void setAutoRotate(boolean autoRotate) {
        this.autoRotate = autoRotate;
    }

    void setYawDegreesListener(IntConsumer yawDegreesListener) {
        this.yawDegreesListener = yawDegreesListener;
        notifyYawChanged();
    }

    private void notifyYawChanged() {
        if (yawDegreesListener != null) {
            yawDegreesListener.accept(currentYawDegrees());
        }
    }

    private int currentYawDegrees() {
        return (int) Math.round(Math.toDegrees(normalizeRadians(yawRadians)));
    }

    void flash(EnumSet<MuscleGroup> groups) {
        long now = System.nanoTime();
        for (MuscleGroup group : groups) {
            flashStartNanos.put(group, now);
        }
    }

    void clearFlashes() {
        flashStartNanos.clear();
    }

    void dispose() {
        animationTimer.stop();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            paintBackground(g);
            if (targetState != null) {
                paintAvatar(g);
            }
        } finally {
            g.dispose();
        }
    }

    private void paintBackground(Graphics2D g) {
        int w = getWidth();
        int h = getHeight();
        g.setPaint(new GradientPaint(0, 0, new Color(24, 34, 52), 0, h, new Color(9, 14, 25)));
        g.fillRect(0, 0, w, h);

        g.setColor(new Color(255, 255, 255, 22));
        int floorY = (int) (h * 0.84);
        for (int i = -8; i <= 8; i++) {
            int x = w / 2 + i * 45;
            g.drawLine(x, floorY, w / 2 + i * 90, h);
        }
        for (int i = 0; i < 7; i++) {
            int y = floorY + i * 24;
            g.drawLine(60, y, w - 60, y);
        }

        g.setColor(new Color(255, 255, 255, 165));
        g.setFont(g.getFont().deriveFont(Font.BOLD, 15f));
        g.drawString("Extreme procedural muscle sandbox", 24, 34);
    }

    private void paintAvatar(Graphics2D g) {
        long now = System.nanoTime();
        double transition = smoothstep(clamp01((double) (now - transitionStartNanos) / TRANSITION_NANOS));
        EnumMap<MuscleGroup, RenderPartState> states = renderStates(transition);
        List<LaidOutPrimitive> layout = solveLayout(states);

        double modelScale = fitScale(layout);
        double centerX = getWidth() * 0.50;
        double centerY = getHeight() * 0.50 + 10.0 * modelScale;

        List<ProjectedPrimitive> projected = new ArrayList<>();
        for (LaidOutPrimitive primitive : layout) {
            projected.add(project(primitive, modelScale, centerX, centerY));
        }
        projected.sort(Comparator.comparingDouble(ProjectedPrimitive::depth));

        paintGroundShadow(g, projected);
        for (ProjectedPrimitive primitive : projected) {
            paintPrimitive(g, primitive, now);
        }
        paintMetrics(g, states);
    }

    private EnumMap<MuscleGroup, RenderPartState> renderStates(double transition) {
        EnumMap<MuscleGroup, RenderPartState> states = new EnumMap<>(MuscleGroup.class);
        for (MuscleGroup group : MuscleGroup.values()) {
            MuscleVisualState start = startState.part(group);
            MuscleVisualState target = targetState.part(group);
            states.put(group, RenderPartState.lerp(start, target, transition));
        }
        return states;
    }

    private List<LaidOutPrimitive> solveLayout(EnumMap<MuscleGroup, RenderPartState> states) {
        RenderPartState chest = states.get(MuscleGroup.CHEST);
        RenderPartState abs = states.get(MuscleGroup.ABS);
        RenderPartState arms = states.get(MuscleGroup.ARMS);
        RenderPartState back = states.get(MuscleGroup.BACK);
        RenderPartState legs = states.get(MuscleGroup.LEGS);
        MuscleDimensions dims = computeMuscleDimensions(states);
        BodyPartAnchors anchors = computeMuscleAnchors(states, dims);

        List<LaidOutPrimitive> result = new ArrayList<>();
        add(result, "head", RenderPartState.neutral(), 0.0, anchors.headY(), 8.0,
                dims.head().width(), dims.head().height(), dims.head().depth());
        add(result, "neck", RenderPartState.neutral(), 0.0, anchors.neckY(), 0.0,
                dims.neck().width(), dims.neck().height(), dims.neck().depth());
        add(result, "pelvis", RenderPartState.neutral(), 0.0, anchors.pelvisY(), 0.0,
                anchors.pelvisWidth(), anchors.pelvisHeight(), dims.pelvis().depth());

        addSymmetric(result, "back-left", "back-right", back, anchors.backX(), anchors.backY(), -34.0, dims.back());
        addSymmetric(result, "lat-left", "lat-right", back, anchors.latX(), anchors.latY(), -22.0, dims.lat());
        addSymmetric(result, "chest-left", "chest-right", chest, anchors.chestX(), anchors.chestY(), 28.0, dims.chest());

        add(result, "abs-upper", abs, 0.0, anchors.absUpperY(), 28.0, dims.absUpper());
        add(result, "abs-mid", abs, 0.0, anchors.absMidY(), 29.0, dims.absMid());
        add(result, "abs-low", abs, 0.0, anchors.absLowY(), 26.0, dims.absLow());

        addSymmetric(result, "deltoid-left", "deltoid-right", arms,
                anchors.deltoidX(), anchors.deltoidY(), 17.0, dims.deltoid());
        addSymmetric(result, "triceps-left", "triceps-right", arms,
                anchors.tricepsX(), anchors.upperArmY(), 8.0, dims.triceps());
        addSymmetric(result, "upper-arm-left", "upper-arm-right", arms,
                anchors.upperArmX(), anchors.upperArmY(), 18.0, dims.upperArm());
        addSymmetric(result, "forearm-left", "forearm-right", arms,
                anchors.forearmX(), anchors.forearmY(), 14.0, dims.forearm());
        addSymmetric(result, "wrist-taper-left", "wrist-taper-right", arms,
                anchors.wristTaperX(), anchors.wristTaperY(), 13.0, dims.wristTaper());
        addSymmetric(result, "hand-left", "hand-right", RenderPartState.neutral(), anchors.handX(),
                anchors.handY(), 12.0, dims.hand());

        addSymmetric(result, "thigh-left", "thigh-right", legs, anchors.thighX(), anchors.thighY(), 7.0, dims.thigh());
        addSymmetric(result, "calf-left", "calf-right", legs, anchors.calfX(), anchors.calfY(), 5.0, dims.calf());
        addSymmetric(result, "foot-left", "foot-right", RenderPartState.neutral(), anchors.footX(),
                anchors.footY(), 25.0, dims.foot());

        return result;
    }

    private MuscleDimensions computeMuscleDimensions(EnumMap<MuscleGroup, RenderPartState> states) {
        RenderPartState arms = states.get(MuscleGroup.ARMS);
        return new MuscleDimensions(
                dimensions("head", RenderPartState.neutral()),
                dimensions("neck", RenderPartState.neutral()),
                dimensions("pelvis", RenderPartState.neutral()),
                dimensions("chest-left", states.get(MuscleGroup.CHEST)),
                dimensions("abs-upper", states.get(MuscleGroup.ABS)),
                dimensions("abs-mid", states.get(MuscleGroup.ABS)),
                dimensions("abs-low", states.get(MuscleGroup.ABS)),
                dimensions("back-left", states.get(MuscleGroup.BACK)),
                dimensions("lat-left", states.get(MuscleGroup.BACK)),
                armDimensions("deltoid-left", arms, 0.86, 0.72, 0.92),
                armDimensions("upper-arm-left", arms, 0.70, 0.88, 0.76),
                armDimensions("triceps-left", arms, 0.64, 0.90, 0.84),
                armDimensions("forearm-left", arms, 0.58, 0.88, 0.62),
                armDimensions("wrist-taper-left", arms, 0.34, 0.64, 0.36),
                dimensions("hand-left", RenderPartState.neutral()),
                dimensions("thigh-left", states.get(MuscleGroup.LEGS)),
                dimensions("calf-left", states.get(MuscleGroup.LEGS)),
                dimensions("foot-left", RenderPartState.neutral())
        );
    }

    private BodyPartAnchors computeMuscleAnchors(EnumMap<MuscleGroup, RenderPartState> states,
                                                MuscleDimensions dims) {
        RenderPartState chest = states.get(MuscleGroup.CHEST);
        RenderPartState abs = states.get(MuscleGroup.ABS);
        RenderPartState arms = states.get(MuscleGroup.ARMS);
        RenderPartState back = states.get(MuscleGroup.BACK);
        RenderPartState legs = states.get(MuscleGroup.LEGS);

        double coreSpacing = LAYOUT_MARGIN + bulkOffset(chest, 4.0) + bulkOffset(abs, 2.5);
        double chestY = -104.0 - bulkOffset(abs, 7.0) + bulkOffset(back, 2.0);
        double chestX = 31.0 + bulkOffset(chest, 16.0);
        double backX = 43.0 + bulkOffset(back, 18.0);
        double chestHalfW = chestX + dims.chest().width() * 0.5;
        double backHalfW = Math.max(backX + dims.back().width() * 0.5, 65.0 + dims.lat().width() * 0.45);
        double absHalfW = widest(dims.absUpper(), dims.absMid(), dims.absLow()) * 0.5;
        double torsoHalfW = Math.max(chestHalfW, Math.max(backHalfW, absHalfW));

        double chestBottom = chestY + dims.chest().height() * 0.5;
        double absUpperY = Math.max(-42.0, chestBottom + coreSpacing + dims.absUpper().height() * 0.5);
        double absMidY = absUpperY + dims.absUpper().height() * 0.5 + coreSpacing + dims.absMid().height() * 0.5;
        double absLowY = absMidY + dims.absMid().height() * 0.5 + coreSpacing + dims.absLow().height() * 0.5;
        double pelvisWidth = clamp(
                Math.max(dims.pelvis().width(), Math.max(absHalfW * 1.18, torsoHalfW * 0.46)),
                dims.pelvis().width(),
                168.0
        );
        double pelvisHeight = Math.max(dims.pelvis().height(), 56.0 + bulkOffset(abs, 10.0));
        double pelvisY = Math.max(92.0, absLowY + dims.absLow().height() * 0.5 + coreSpacing + pelvisHeight * 0.5);

        double armEnvelopeW = Math.max(dims.deltoid().width(), Math.max(dims.upperArm().width(), dims.triceps().width()));
        double shoulderX = torsoHalfW + Math.min(armEnvelopeW * 0.26, 54.0) + bulkOffset(arms, 5.0) + 8.0;
        double shoulderY = chestY - dims.chest().height() * 0.15 + bulkOffset(back, 2.5);
        double legChain = byName("thigh-left").height() * limbLengthScale(legs)
                + byName("calf-left").height() * limbLengthScale(legs);
        double armChainLimit = legChain * 0.82;
        double upperArmBone = byName("upper-arm-left").height() * limbLengthScale(arms);
        double forearmBone = byName("forearm-left").height() * limbLengthScale(arms);
        double armRatio = Math.min(1.0, armChainLimit / Math.max(1.0, upperArmBone + forearmBone));
        upperArmBone *= armRatio;
        forearmBone *= armRatio;
        double elbowY = shoulderY + upperArmBone * 0.88;
        double wristY = elbowY + forearmBone * 0.86;
        double upperArmY = (shoulderY + elbowY) * 0.5;
        double forearmY = (elbowY + wristY) * 0.5;

        double deltoidX = Math.max(torsoHalfW + dims.deltoid().width() * 0.22, shoulderX - dims.deltoid().width() * 0.18);
        double armPush = Math.min(armEnvelopeW * 0.12, 30.0);
        double tricepsX = shoulderX + armPush;
        double upperArmX = shoulderX + Math.min(armEnvelopeW * 0.07, 18.0);
        double forearmX = shoulderX + Math.min(armEnvelopeW * 0.12, 26.0);
        double wristTaperX = shoulderX + Math.min(armEnvelopeW * 0.16, 32.0);
        double handX = wristTaperX + Math.min(dims.hand().width() * 0.52, 22.0);
        double handY = wristY + Math.min(22.0, dims.hand().height() * 0.58);

        double hipEdge = Math.max(pelvisWidth * 0.42, torsoHalfW * 0.30);
        double legPush = Math.min(Math.max(dims.thigh().width(), dims.calf().width()) * 0.16, 36.0);
        double thighX = Math.min(hipEdge + legPush, torsoHalfW + 46.0);
        double thighY = pelvisY + pelvisHeight * 0.40 + dims.thigh().height() * 0.48;
        double calfX = thighX + Math.min(5.0 + bulkOffset(legs, 2.0), 12.0);
        double calfY = thighY + dims.thigh().height() * 0.48 + LAYOUT_MARGIN + dims.calf().height() * 0.48;
        double footX = calfX + 6.0;
        double footY = calfY + dims.calf().height() * 0.52 + 18.0;

        double backY = chestY + 16.0 + bulkOffset(abs, 2.0);
        double latX = Math.max(65.0, torsoHalfW - dims.lat().width() * 0.40);
        double latY = absUpperY - dims.absUpper().height() * 0.25;
        double torsoTop = Math.min(chestY - dims.chest().height() * 0.5, backY - dims.back().height() * 0.5);
        double neckY = torsoTop - dims.neck().height() * 0.42;
        double headY = neckY - dims.neck().height() * 0.48 - dims.head().height() * 0.46;

        return new BodyPartAnchors(
                chestX, chestY,
                backX, backY,
                latX, latY,
                absUpperY, absMidY, absLowY,
                pelvisY, pelvisWidth, pelvisHeight,
                shoulderX, shoulderY, elbowY, wristY,
                deltoidX, shoulderY + upperArmBone * 0.10,
                tricepsX, upperArmX, upperArmY,
                forearmX, forearmY,
                wristTaperX, wristY - dims.wristTaper().height() * 0.24,
                handX, handY,
                thighX, thighY,
                calfX, calfY,
                footX, footY,
                neckY, headY,
                torsoHalfW
        );
    }

    private void addSymmetric(List<LaidOutPrimitive> result,
                              String leftName,
                              String rightName,
                              RenderPartState state,
                              double halfX,
                              double y,
                              double z,
                              Dimensions dimensions) {
        add(result, leftName, state, -halfX, y, z, dimensions);
        add(result, rightName, state, halfX, y, z, dimensions);
    }

    private void add(List<LaidOutPrimitive> result,
                     String name,
                     RenderPartState state,
                     double x,
                     double y,
                     double z,
                     Dimensions dimensions) {
        add(result, name, state, x, y, z, dimensions.width(), dimensions.height(), dimensions.depth());
    }

    private void add(List<LaidOutPrimitive> result,
                     String name,
                     RenderPartState state,
                     double x,
                     double y,
                     double z,
                     double width,
                     double height,
                     double depth) {
        BodyPrimitive base = byName(name);
        result.add(new LaidOutPrimitive(base, state, x, y, z, width, height, depth, base.angleRadians()));
    }

    private Dimensions dimensions(String primitiveName, RenderPartState state) {
        BodyPrimitive primitive = byName(primitiveName);
        return new Dimensions(
                primitive.width() * visualWidthScale(primitive, state),
                primitive.height() * cappedHeightScale(primitive, state),
                primitive.depth() * visualDepthScale(primitive, state)
        );
    }

    private Dimensions armDimensions(String primitiveName,
                                     RenderPartState state,
                                     double widthFactor,
                                     double heightFactor,
                                     double depthFactor) {
        BodyPrimitive primitive = byName(primitiveName);
        return new Dimensions(
                primitive.width() * armThicknessScale(state) * widthFactor,
                primitive.height() * limbLengthScale(state) * heightFactor,
                primitive.depth() * armDepthScale(state) * depthFactor
        );
    }

    private static double cappedHeightScale(BodyPrimitive primitive, RenderPartState state) {
        if (primitive.group() == MuscleGroup.ARMS || primitive.group() == MuscleGroup.LEGS) {
            return limbLengthScale(state);
        }
        if (primitive.group() == MuscleGroup.CHEST
                || primitive.group() == MuscleGroup.ABS
                || primitive.group() == MuscleGroup.BACK) {
            return 1.0 + Math.min(Math.max(0.0, state.scaleY() - 1.0), MAX_CORE_LENGTH_GROWTH);
        }
        return state.scaleY();
    }

    private static double limbLengthScale(RenderPartState state) {
        return 1.0 + Math.min(Math.max(0.0, state.scaleY() - 1.0), MAX_LIMB_LENGTH_GROWTH);
    }

    private static double armThicknessScale(RenderPartState state) {
        return Math.min(6.4, 1.0 + Math.sqrt(Math.max(0.0, state.scaleX() - 1.0)) * 0.95);
    }

    private static double armDepthScale(RenderPartState state) {
        return Math.min(6.1, 1.0 + Math.sqrt(Math.max(0.0, state.scaleZ() - 1.0)) * 0.90);
    }

    private static double visualWidthScale(BodyPrimitive primitive, RenderPartState state) {
        if (primitive.group() == MuscleGroup.ARMS) {
            return armThicknessScale(state);
        }
        if (primitive.group() == MuscleGroup.LEGS) {
            return legThicknessScale(state);
        }
        if (primitive.group() == MuscleGroup.CHEST
                || primitive.group() == MuscleGroup.ABS
                || primitive.group() == MuscleGroup.BACK) {
            return coreThicknessScale(state);
        }
        return state.scaleX();
    }

    private static double visualDepthScale(BodyPrimitive primitive, RenderPartState state) {
        if (primitive.group() == MuscleGroup.ARMS) {
            return armDepthScale(state);
        }
        if (primitive.group() == MuscleGroup.LEGS) {
            return legDepthScale(state);
        }
        if (primitive.group() == MuscleGroup.CHEST
                || primitive.group() == MuscleGroup.ABS
                || primitive.group() == MuscleGroup.BACK) {
            return coreDepthScale(state);
        }
        return state.scaleZ();
    }

    private static double legThicknessScale(RenderPartState state) {
        return Math.min(5.2, 1.0 + Math.sqrt(Math.max(0.0, state.scaleX() - 1.0)) * 0.78);
    }

    private static double legDepthScale(RenderPartState state) {
        return Math.min(5.0, 1.0 + Math.sqrt(Math.max(0.0, state.scaleZ() - 1.0)) * 0.74);
    }

    private static double coreThicknessScale(RenderPartState state) {
        return Math.min(5.6, 1.0 + Math.sqrt(Math.max(0.0, state.scaleX() - 1.0)) * 0.86);
    }

    private static double coreDepthScale(RenderPartState state) {
        return Math.min(6.0, 1.0 + Math.sqrt(Math.max(0.0, state.scaleZ() - 1.0)) * 0.92);
    }

    private static double bulkOffset(RenderPartState state, double maxOffset) {
        double bulk = Math.max(
                Math.max(visualWidthOnlyScale(state), visualDepthOnlyScale(state)),
                1.0 + Math.max(0.0, state.scaleY() - 1.0)
        );
        return Math.min(maxOffset, Math.max(0.0, bulk - 1.0) * maxOffset * 0.34);
    }

    private static double visualWidthOnlyScale(RenderPartState state) {
        return 1.0 + Math.sqrt(Math.max(0.0, state.scaleX() - 1.0));
    }

    private static double visualDepthOnlyScale(RenderPartState state) {
        return 1.0 + Math.sqrt(Math.max(0.0, state.scaleZ() - 1.0));
    }

    private static double widest(Dimensions first, Dimensions second, Dimensions third) {
        return Math.max(first.width(), Math.max(second.width(), third.width()));
    }

    private BodyPrimitive byName(String name) {
        for (BodyPrimitive primitive : primitives) {
            if (primitive.name().equals(name)) {
                return primitive;
            }
        }
        throw new IllegalArgumentException("Unknown primitive: " + name);
    }

    private double fitScale(List<LaidOutPrimitive> layout) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double cos = Math.cos(yawRadians);
        double sin = Math.sin(yawRadians);

        for (LaidOutPrimitive primitive : layout) {
            double projectedHalfW = (Math.abs(primitive.width() * cos) + Math.abs(primitive.depth() * sin) * 0.82) * 0.5;
            minX = Math.min(minX, primitive.x() - projectedHalfW);
            maxX = Math.max(maxX, primitive.x() + projectedHalfW);
            minY = Math.min(minY, primitive.y() - primitive.height() * 0.5);
            maxY = Math.max(maxY, primitive.y() + primitive.height() * 0.5);
        }

        double availableW = Math.max(100.0, getWidth() - 84.0);
        double availableH = Math.max(100.0, getHeight() - 104.0);
        double layoutW = Math.max(1.0, maxX - minX);
        double layoutH = Math.max(1.0, maxY - minY);
        return Math.min(availableW / layoutW, availableH / layoutH);
    }

    private ProjectedPrimitive project(LaidOutPrimitive primitive, double modelScale, double centerX, double centerY) {
        double cos = Math.cos(yawRadians);
        double sin = Math.sin(yawRadians);
        double rotatedX = primitive.x() * cos + primitive.z() * sin;
        double rotatedZ = primitive.z() * cos - primitive.x() * sin;
        double perspective = Math.max(0.30, 1.0 + rotatedZ * 0.0018);
        double x = centerX + rotatedX * modelScale * perspective;
        double y = centerY + primitive.y() * modelScale;
        double width = (Math.abs(primitive.width() * cos) + Math.abs(primitive.depth() * sin) * 0.82) * modelScale * perspective;
        double height = primitive.height() * modelScale * perspective;
        double angle = primitive.angleRadians() * Math.signum(cos == 0.0 ? 1.0 : cos);
        return new ProjectedPrimitive(primitive, x, y, rotatedZ, Math.max(3.0, width), Math.max(3.0, height), angle);
    }

    private void paintGroundShadow(Graphics2D g, List<ProjectedPrimitive> projected) {
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (ProjectedPrimitive primitive : projected) {
            minX = Math.min(minX, primitive.x() - primitive.width() * 0.45);
            maxX = Math.max(maxX, primitive.x() + primitive.width() * 0.45);
            maxY = Math.max(maxY, primitive.y() + primitive.height() * 0.5);
        }
        double w = Math.max(80.0, (maxX - minX) * 0.78);
        double h = Math.max(20.0, w * 0.16);
        double x = (minX + maxX) * 0.5 - w * 0.5;
        double y = maxY - h * 0.22;
        g.setColor(new Color(0, 0, 0, 95));
        g.fill(new Ellipse2D.Double(x, y, w, h));
    }

    private void paintPrimitive(Graphics2D g, ProjectedPrimitive projected, long now) {
        LaidOutPrimitive laidOut = projected.primitive();
        BodyPrimitive primitive = laidOut.primitive();
        RenderPartState state = laidOut.state();
        double flash = flashAmount(primitive.group(), now);
        Color fill = muscleColor(primitive.baseColor(), state.material(), flash);
        Shape outline = primitive.kind() == PrimitiveKind.CAPSULE
                ? capsule(projected.x(), projected.y(), projected.width(), projected.height(), projected.angleRadians())
                : ellipse(projected.x(), projected.y(), projected.width(), projected.height());

        if (SHOW_DEBUG_OUTLINES && flash > 0.0) {
            g.setColor(new Color(255, 232, 156, (int) Math.round(120.0 * flash)));
            g.setStroke(new BasicStroke((float) (8.0 * flash), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(outline);
        }

        BufferedImage shaded = renderNormalMappedPrimitive(
                primitive.kind(),
                primitive.group(),
                projected.width(),
                projected.height(),
                fill,
                state.material(),
                surfaceDetailAmount(primitive.group(), state),
                flash
        );
        drawTexturedShape(g, shaded, projected.x(), projected.y(), projected.width(), projected.height(), projected.angleRadians());

        if (SHOW_DEBUG_OUTLINES) {
            g.setColor(darken(fill, 0.42));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(outline);
        }

        if ("head".equals(primitive.name())) {
            paintFace(g, projected);
        }
    }

    private BufferedImage renderNormalMappedPrimitive(PrimitiveKind kind,
                                                     MuscleGroup group,
                                                     double targetWidth,
                                                     double targetHeight,
                                                     Color base,
                                                     MaterialVisualState material,
                                                     double detail,
                                                     double flash) {
        int imageW = Math.max(8, Math.min(MAX_TEXTURE_SIDE, (int) Math.round(targetWidth)));
        int imageH = Math.max(8, Math.min(MAX_TEXTURE_SIDE, (int) Math.round(targetHeight)));
        BufferedImage image = new BufferedImage(imageW, imageH, BufferedImage.TYPE_INT_ARGB);
        double definition = clamp01(material.normalBlend01() * (0.28 + detail * 0.92));
        double veinLevel = clamp01((material.vascularity01() + material.pump01() * 0.55) * smoothstep(detail));
        double[] light = normalize3(-0.48, -0.58, 0.66);
        double[] view = {0.0, 0.0, 1.0};

        for (int y = 0; y < imageH; y++) {
            double v = ((y + 0.5) / imageH) * 2.0 - 1.0;
            for (int x = 0; x < imageW; x++) {
                double u = ((x + 0.5) / imageW) * 2.0 - 1.0;
                MaskSample mask = mask(kind, u, v, imageW, imageH);
                if (mask.alpha() <= 0.0) {
                    continue;
                }

                double baseNz = Math.sqrt(Math.max(0.0, 1.0 - mask.nx() * mask.nx() - mask.ny() * mask.ny()));
                double h = muscleHeight(group, u, v, definition, veinLevel, detail);
                double hx = muscleHeight(group, u + 0.012, v, definition, veinLevel, detail) - h;
                double hy = muscleHeight(group, u, v + 0.012, definition, veinLevel, detail) - h;
                double strength = 1.1 + 3.2 * detail + 0.8 * material.pump01();
                double[] normal = normalize3(mask.nx() - hx * strength, mask.ny() - hy * strength, baseNz);

                double diffuse = Math.max(0.0, dot(normal, light));
                double rim = Math.pow(Math.max(0.0, 1.0 - dot(normal, view)), 2.2);
                double spec = Math.pow(Math.max(0.0, dot(reflect(light, normal), view)), 26.0) * material.specular01();
                double vein = veinMask(group, u, v) * veinLevel;
                double highlight = muscleHighlight(group, u, v) * detail;
                double grooveShadow = muscleGrooveShadow(group, u, v) * detail;

                Color local = mix(base, new Color(92, 126, 155), vein * 0.42);
                local = mix(local, new Color(255, 211, 163), highlight * 0.16);
                local = mix(local, new Color(95, 55, 48), grooveShadow * 0.18);
                double shade = 0.45 + diffuse * (0.52 + detail * 0.20) + rim * (0.14 + detail * 0.16)
                        + highlight * 0.16 - grooveShadow * 0.18 + flash * 0.16;
                int r = clampColor((int) Math.round(local.getRed() * shade + spec * 120.0));
                int gr = clampColor((int) Math.round(local.getGreen() * shade + spec * 110.0));
                int b = clampColor((int) Math.round(local.getBlue() * shade + spec * 130.0));
                int a = clampColor((int) Math.round(mask.alpha() * 255.0));
                image.setRGB(x, y, (a << 24) | (r << 16) | (gr << 8) | b);
            }
        }
        return image;
    }

    private void drawTexturedShape(Graphics2D g,
                                   BufferedImage image,
                                   double centerX,
                                   double centerY,
                                   double width,
                                   double height,
                                   double angleRadians) {
        AffineTransform transform = new AffineTransform();
        transform.translate(centerX, centerY);
        transform.rotate(angleRadians);
        transform.scale(width / image.getWidth(), height / image.getHeight());
        transform.translate(-image.getWidth() / 2.0, -image.getHeight() / 2.0);
        g.drawImage(image, transform, null);
    }

    private void paintFace(Graphics2D g, ProjectedPrimitive projected) {
        if (Math.cos(yawRadians) < -0.08) {
            return;
        }

        double w = projected.width();
        double h = projected.height();
        double faceTurn = Math.sin(yawRadians) * 0.16 * w;
        Graphics2D face = (Graphics2D) g.create();
        try {
            face.translate(projected.x() + faceTurn, projected.y());
            face.rotate(projected.angleRadians());
            face.setColor(new Color(33, 38, 49));
            face.fill(new Ellipse2D.Double(-w * 0.23, -h * 0.12, w * 0.10, h * 0.13));
            face.fill(new Ellipse2D.Double(w * 0.13, -h * 0.12, w * 0.10, h * 0.13));
            face.setColor(new Color(255, 255, 255, 230));
            face.fill(new Ellipse2D.Double(-w * 0.20, -h * 0.10, w * 0.025, h * 0.035));
            face.fill(new Ellipse2D.Double(w * 0.16, -h * 0.10, w * 0.025, h * 0.035));
            face.setColor(new Color(118, 38, 55));
            face.setStroke(new BasicStroke((float) Math.max(2.0, w * 0.035), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            face.draw(new Arc2D.Double(-w * 0.27, -h * 0.03, w * 0.54, h * 0.36, 205, 130, Arc2D.OPEN));
        } finally {
            face.dispose();
        }
    }

    private void paintMetrics(Graphics2D g, EnumMap<MuscleGroup, RenderPartState> states) {
        g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
        int y = getHeight() - 98;
        g.setColor(new Color(255, 255, 255, 188));
        g.drawString("Anchored proportions. Bulk grows through width and depth.", 24, y);
        y += 18;
        for (MuscleGroup group : MuscleGroup.values()) {
            RenderPartState state = states.get(group);
            String text = String.format(Locale.US, "%s scale X/Z %.1fx / %.1fx",
                    group.displayName(), state.scaleX(), state.scaleZ());
            g.drawString(text, 24, y);
            y += 16;
        }
    }

    private double flashAmount(MuscleGroup group, long now) {
        if (group == null || !flashStartNanos.containsKey(group)) {
            return 0.0;
        }
        double elapsed = (double) (now - flashStartNanos.get(group)) / FLASH_NANOS;
        if (elapsed >= 1.0) {
            flashStartNanos.remove(group);
            return 0.0;
        }
        return 1.0 - smoothstep(elapsed);
    }

    private static Shape ellipse(double centerX, double centerY, double width, double height) {
        return new Ellipse2D.Double(centerX - width / 2.0, centerY - height / 2.0, width, height);
    }

    private static Shape capsule(double centerX, double centerY, double width, double height, double angleRadians) {
        double arc = Math.min(width, height);
        Shape shape = new RoundRectangle2D.Double(-width / 2.0, -height / 2.0, width, height, arc, arc);
        AffineTransform transform = new AffineTransform();
        transform.translate(centerX, centerY);
        transform.rotate(angleRadians);
        return transform.createTransformedShape(shape);
    }

    private static MaskSample mask(PrimitiveKind kind, double u, double v, int width, int height) {
        if (kind == PrimitiveKind.ELLIPSOID) {
            double r2 = u * u + v * v;
            if (r2 > 1.08) {
                return MaskSample.empty();
            }
            double alpha = clamp01((1.08 - r2) / 0.08);
            return new MaskSample(alpha, u * 0.78, v * 0.78);
        }

        double radiusPixels = Math.min(width, height) * 0.5;
        double halfW = width * 0.5;
        double halfH = height * 0.5;
        double px = u * halfW;
        double py = v * halfH;
        double rectHalfH = Math.max(0.0, halfH - radiusPixels);
        double closestY = clamp(py, -rectHalfH, rectHalfH);
        double dx = px;
        double dy = py - closestY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > radiusPixels + 1.5) {
            return MaskSample.empty();
        }
        double alpha = clamp01((radiusPixels + 1.5 - dist) / 2.5);
        double nx = radiusPixels == 0.0 ? 0.0 : dx / radiusPixels;
        double ny = radiusPixels == 0.0 ? 0.0 : dy / radiusPixels;
        return new MaskSample(alpha, nx * 0.82, ny * 0.82);
    }

    private static double surfaceDetailAmount(MuscleGroup group, RenderPartState state) {
        if (group == null) {
            return 0.0;
        }
        double scale = Math.max(state.scaleX(), Math.max(state.scaleY(), state.scaleZ()));
        double scaleDetail = smoothstep(clamp((scale - 1.35) / 4.25, 0.0, 1.0));
        double materialDetail = smoothstep(clamp((state.material().normalBlend01() - 0.18) / 0.72, 0.0, 1.0));
        return clamp01(scaleDetail * 0.72 + materialDetail * 0.28);
    }

    private static double muscleHeight(MuscleGroup group,
                                       double u,
                                       double v,
                                       double definition,
                                       double veinLevel,
                                       double detail) {
        if (group == null) {
            return 0.012 * Math.sin(u * 6.0) * Math.cos(v * 5.0);
        }

        double fiber = 0.0;
        double soft = smoothstep(clamp(detail * 1.35, 0.0, 1.0));
        double hard = smoothstep(clamp((detail - 0.38) / 0.62, 0.0, 1.0));
        switch (group) {
            case CHEST -> {
                double split = -groove(Math.abs(u), 0.055, 0.020);
                double lowerArc = ridge(v + 0.20 + 0.20 * Math.abs(u), 0.045);
                double clavicle = ridge(v + 0.52 - Math.abs(u) * 0.22, 0.055) * 0.32;
                double fanFiber = Math.sin((u + Math.signum(u) * 0.18) * 18.0 + v * 8.0) * 0.012 * soft;
                double hardStriation = Math.sin((u + Math.signum(u) * 0.28) * 32.0 + v * 13.0) * 0.010 * hard;
                fiber = split * (0.85 + hard * 0.45) + lowerArc * (0.45 + soft * 0.45)
                        + clavicle * soft + fanFiber + hardStriation;
            }
            case ABS -> {
                double center = -groove(Math.abs(u), 0.050, 0.020);
                double rows = 0.0;
                for (double row : new double[]{-0.42, -0.12, 0.18, 0.48}) {
                    rows -= groove(Math.abs(v - row), 0.035, 0.015);
                }
                double oblique = ridge(Math.abs(u) - (0.42 + v * 0.08), 0.060) * 0.32;
                double tendon = Math.sin(v * 34.0) * 0.007 * hard;
                fiber = center * (0.75 + hard * 0.50) + rows * (0.55 + soft * 0.50)
                        + oblique * soft + Math.sin(u * 26.0) * 0.007 * soft + tendon;
            }
            case ARMS -> {
                double longFiber = Math.sin(v * 20.0 + u * 7.0) * 0.012 * soft;
                double bicep = ridge(u * 0.62 + Math.sin(v * 2.5) * 0.18, 0.23) * (0.42 + soft * 0.24);
                double tricepsCut = -groove(Math.abs(u + 0.42 + Math.sin(v * 2.0) * 0.10), 0.070, 0.025) * hard;
                double forearmCord = ridge(Math.abs(u - 0.22 * Math.sin(v * 2.8)) - 0.24, 0.050) * 0.24 * hard;
                fiber = longFiber + bicep + tricepsCut + forearmCord;
            }
            case BACK -> {
                double spine = -groove(Math.abs(u), 0.045, 0.020) * (0.8 + hard * 0.5);
                double lat = ridge(Math.abs(u) - (0.35 + v * 0.12), 0.12) * (0.45 + soft * 0.30);
                double fan = Math.sin(Math.abs(u) * 24.0 - v * 13.0) * 0.012 * soft;
                double lowerTrap = ridge(v + 0.08 + Math.abs(u) * 0.42, 0.070) * 0.22 * hard;
                fiber = spine + lat + fan + lowerTrap;
            }
            case LEGS -> {
                double quadSplit = -groove(Math.abs(u - 0.18 * Math.sin(v * 2.0)), 0.070, 0.025) * (0.75 + hard * 0.45);
                double longFiber = Math.sin(v * 23.0 - u * 6.5) * 0.012 * soft;
                double teardrop = ridge(u + 0.36, 0.20) * (0.18 + soft * 0.20);
                double outerSweep = ridge(u - 0.40 + Math.sin(v * 1.8) * 0.08, 0.12) * 0.18 * hard;
                fiber = quadSplit + longFiber + teardrop + outerSweep;
            }
        }

        return fiber * definition + veinMask(group, u, v) * 0.075 * veinLevel * hard;
    }

    private static double muscleHighlight(MuscleGroup group, double u, double v) {
        if (group == null) {
            return 0.0;
        }
        return switch (group) {
            case CHEST -> ridge(u + 0.28, 0.22) * ridge(v + 0.18, 0.32)
                    + ridge(u - 0.30, 0.24) * ridge(v + 0.24, 0.30);
            case ABS -> ridge(Math.abs(u) - 0.24, 0.12) * ridge(v - 0.02, 0.72);
            case ARMS -> ridge(u - 0.10, 0.22) * ridge(v + 0.08, 0.74);
            case BACK -> ridge(Math.abs(u) - 0.42, 0.16) * ridge(v + 0.05, 0.62);
            case LEGS -> ridge(u + 0.22, 0.18) * ridge(v + 0.04, 0.78);
        };
    }

    private static double muscleGrooveShadow(MuscleGroup group, double u, double v) {
        if (group == null) {
            return 0.0;
        }
        double groove = 0.0;
        switch (group) {
            case CHEST -> {
                groove += ridge(Math.abs(u), 0.050) * ridge(v + 0.04, 0.70);
                groove += ridge(v + 0.22 + Math.abs(u) * 0.16, 0.055);
            }
            case ABS -> {
                groove += ridge(Math.abs(u), 0.050);
                for (double row : new double[]{-0.42, -0.12, 0.18, 0.48}) {
                    groove += ridge(v - row, 0.040);
                }
            }
            case ARMS -> {
                groove += ridge(u + 0.34 + Math.sin(v * 2.4) * 0.10, 0.080);
                groove += ridge(Math.abs(u - 0.24 * Math.sin(v * 3.0)) - 0.34, 0.045);
            }
            case BACK -> {
                groove += ridge(Math.abs(u), 0.045);
                groove += ridge(Math.abs(u) - (0.34 + v * 0.12), 0.075);
            }
            case LEGS -> {
                groove += ridge(u - 0.16 * Math.sin(v * 2.0), 0.070);
                groove += ridge(Math.abs(u) - 0.42, 0.070);
            }
        }
        return clamp01(groove);
    }

    private static double veinMask(MuscleGroup group, double u, double v) {
        if (group == null) {
            return 0.0;
        }
        double vein = 0.0;
        switch (group) {
            case CHEST -> {
                vein += veinCurve(u, v, -0.28, -0.10, 0.23, 0.38, 0.035);
                vein += veinCurve(u, v, 0.30, -0.14, -0.24, 0.32, 0.032);
            }
            case ABS -> {
                vein += veinCurve(u, v, -0.22, -0.55, 0.18, 0.95, 0.030);
                vein += veinCurve(u, v, 0.24, -0.35, -0.12, 0.82, 0.026);
            }
            case ARMS -> vein += veinCurve(u, v, -0.20, -0.82, 0.36, 1.48, 0.034);
            case BACK -> {
                vein += veinCurve(u, v, -0.34, -0.42, 0.18, 0.84, 0.030);
                vein += veinCurve(u, v, 0.34, -0.42, -0.18, 0.84, 0.030);
            }
            case LEGS -> vein += veinCurve(u, v, 0.18, -0.82, -0.26, 1.55, 0.034);
        }
        return clamp01(vein);
    }

    private static double veinCurve(double u,
                                    double v,
                                    double startU,
                                    double startV,
                                    double deltaU,
                                    double deltaV,
                                    double width) {
        double t = clamp01(((u - startU) * deltaU + (v - startV) * deltaV) / (deltaU * deltaU + deltaV * deltaV));
        double curveU = startU + deltaU * t + Math.sin(t * Math.PI * 3.0) * 0.035;
        double curveV = startV + deltaV * t;
        double dist = Math.hypot(u - curveU, v - curveV);
        return Math.exp(-(dist * dist) / (width * width));
    }

    private static double ridge(double value, double width) {
        return Math.exp(-(value * value) / Math.max(0.0001, width * width));
    }

    private static double groove(double value, double width, double falloff) {
        double inner = Math.exp(-(value * value) / Math.max(0.0001, width * width));
        double outer = Math.exp(-(value * value) / Math.max(0.0001, (width + falloff) * (width + falloff)));
        return clamp01(inner * 1.15 - outer * 0.35);
    }

    private static Color muscleColor(Color base, MaterialVisualState material, double flash) {
        double redPump = material.pump01() * 0.32;
        double sheen = material.specular01() * 0.08 + flash * 0.16;
        int r = clampColor(base.getRed() + (int) Math.round(74.0 * redPump + 68.0 * sheen));
        int g = clampColor(base.getGreen() + (int) Math.round(20.0 * sheen) - (int) Math.round(16.0 * redPump));
        int b = clampColor(base.getBlue() + (int) Math.round(24.0 * sheen) - (int) Math.round(22.0 * redPump));
        return new Color(r, g, b);
    }

    private static Color darken(Color color, double amount) {
        return mix(color, Color.BLACK, amount);
    }

    private static Color mix(Color a, Color b, double amount) {
        double t = clamp01(amount);
        int r = (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(clampColor(r), clampColor(g), clampColor(bl));
    }

    private static double[] normalize3(double x, double y, double z) {
        double len = Math.sqrt(x * x + y * y + z * z);
        if (len == 0.0) {
            return new double[]{0.0, 0.0, 1.0};
        }
        return new double[]{x / len, y / len, z / len};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] reflect(double[] incident, double[] normal) {
        double d = dot(incident, normal);
        return normalize3(
                incident[0] - 2.0 * d * normal[0],
                incident[1] - 2.0 * d * normal[1],
                incident[2] - 2.0 * d * normal[2]
        );
    }

    private static int clampColor(int value) {
        return Math.min(255, Math.max(0, value));
    }

    private static double smoothstep(double value) {
        double x = clamp01(value);
        return x * x * (3.0 - 2.0 * x);
    }

    private static double normalizeRadians(double radians) {
        double normalized = Math.IEEEremainder(radians, Math.PI * 2.0);
        if (normalized <= -Math.PI) {
            normalized += Math.PI * 2.0;
        }
        if (normalized > Math.PI) {
            normalized -= Math.PI * 2.0;
        }
        return normalized;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    private static List<BodyPrimitive> buildPrimitives() {
        Color skin = new Color(206, 157, 119);
        Color chest = new Color(208, 116, 91);
        Color abs = new Color(207, 149, 94);
        Color arms = new Color(201, 137, 101);
        Color back = new Color(151, 128, 189);
        Color legs = new Color(174, 129, 92);
        Color neutral = new Color(184, 151, 118);

        List<BodyPrimitive> parts = new ArrayList<>();
        parts.add(new BodyPrimitive("head", null, PrimitiveKind.ELLIPSOID, 0, -235, 8, 58, 70, 45, 0, skin));
        parts.add(new BodyPrimitive("neck", null, PrimitiveKind.CAPSULE, 0, -185, 0, 34, 52, 28, 0, neutral));
        parts.add(new BodyPrimitive("pelvis", null, PrimitiveKind.ELLIPSOID, 0, 92, 0, 92, 70, 54, 0, neutral));

        parts.add(new BodyPrimitive("back-left", MuscleGroup.BACK, PrimitiveKind.ELLIPSOID, -43, -79, -28, 66, 150, 46, -0.08, back));
        parts.add(new BodyPrimitive("back-right", MuscleGroup.BACK, PrimitiveKind.ELLIPSOID, 43, -79, -28, 66, 150, 46, 0.08, back));
        parts.add(new BodyPrimitive("lat-left", MuscleGroup.BACK, PrimitiveKind.ELLIPSOID, -65, -40, -18, 44, 126, 40, -0.12, back));
        parts.add(new BodyPrimitive("lat-right", MuscleGroup.BACK, PrimitiveKind.ELLIPSOID, 65, -40, -18, 44, 126, 40, 0.12, back));

        parts.add(new BodyPrimitive("chest-left", MuscleGroup.CHEST, PrimitiveKind.ELLIPSOID, -31, -102, 24, 78, 86, 52, -0.03, chest));
        parts.add(new BodyPrimitive("chest-right", MuscleGroup.CHEST, PrimitiveKind.ELLIPSOID, 31, -102, 24, 78, 86, 52, 0.03, chest));

        parts.add(new BodyPrimitive("abs-upper", MuscleGroup.ABS, PrimitiveKind.ELLIPSOID, 0, -42, 25, 72, 58, 34, 0, abs));
        parts.add(new BodyPrimitive("abs-mid", MuscleGroup.ABS, PrimitiveKind.ELLIPSOID, 0, 6, 26, 66, 54, 32, 0, abs));
        parts.add(new BodyPrimitive("abs-low", MuscleGroup.ABS, PrimitiveKind.ELLIPSOID, 0, 52, 23, 62, 50, 30, 0, abs));

        parts.add(new BodyPrimitive("deltoid-left", MuscleGroup.ARMS, PrimitiveKind.ELLIPSOID, -96, -88, 16, 58, 62, 48, -0.18, arms));
        parts.add(new BodyPrimitive("deltoid-right", MuscleGroup.ARMS, PrimitiveKind.ELLIPSOID, 96, -88, 16, 58, 62, 48, 0.18, arms));
        parts.add(new BodyPrimitive("upper-arm-left", MuscleGroup.ARMS, PrimitiveKind.CAPSULE, -106, -76, 18, 42, 118, 38, -0.22, arms));
        parts.add(new BodyPrimitive("upper-arm-right", MuscleGroup.ARMS, PrimitiveKind.CAPSULE, 106, -76, 18, 42, 118, 38, 0.22, arms));
        parts.add(new BodyPrimitive("triceps-left", MuscleGroup.ARMS, PrimitiveKind.CAPSULE, -116, -70, 8, 40, 120, 42, -0.18, arms));
        parts.add(new BodyPrimitive("triceps-right", MuscleGroup.ARMS, PrimitiveKind.CAPSULE, 116, -70, 8, 40, 120, 42, 0.18, arms));
        parts.add(new BodyPrimitive("forearm-left", MuscleGroup.ARMS, PrimitiveKind.CAPSULE, -126, 27, 13, 35, 112, 32, -0.10, arms));
        parts.add(new BodyPrimitive("forearm-right", MuscleGroup.ARMS, PrimitiveKind.CAPSULE, 126, 27, 13, 35, 112, 32, 0.10, arms));
        parts.add(new BodyPrimitive("wrist-taper-left", MuscleGroup.ARMS, PrimitiveKind.CAPSULE, -132, 78, 12, 25, 64, 24, -0.06, arms));
        parts.add(new BodyPrimitive("wrist-taper-right", MuscleGroup.ARMS, PrimitiveKind.CAPSULE, 132, 78, 12, 25, 64, 24, 0.06, arms));
        parts.add(new BodyPrimitive("hand-left", null, PrimitiveKind.ELLIPSOID, -130, 101, 12, 35, 32, 25, 0, skin));
        parts.add(new BodyPrimitive("hand-right", null, PrimitiveKind.ELLIPSOID, 130, 101, 12, 35, 32, 25, 0, skin));

        parts.add(new BodyPrimitive("thigh-left", MuscleGroup.LEGS, PrimitiveKind.CAPSULE, -35, 166, 7, 52, 150, 42, 0.05, legs));
        parts.add(new BodyPrimitive("thigh-right", MuscleGroup.LEGS, PrimitiveKind.CAPSULE, 35, 166, 7, 52, 150, 42, -0.05, legs));
        parts.add(new BodyPrimitive("calf-left", MuscleGroup.LEGS, PrimitiveKind.CAPSULE, -38, 282, 5, 42, 128, 35, -0.03, legs));
        parts.add(new BodyPrimitive("calf-right", MuscleGroup.LEGS, PrimitiveKind.CAPSULE, 38, 282, 5, 42, 128, 35, 0.03, legs));
        parts.add(new BodyPrimitive("foot-left", null, PrimitiveKind.ELLIPSOID, -45, 360, 25, 66, 30, 44, -0.06, skin));
        parts.add(new BodyPrimitive("foot-right", null, PrimitiveKind.ELLIPSOID, 45, 360, 25, 66, 30, 44, 0.06, skin));
        return List.copyOf(parts);
    }

    private enum PrimitiveKind {
        ELLIPSOID,
        CAPSULE
    }

    private record BodyPrimitive(
            String name,
            MuscleGroup group,
            PrimitiveKind kind,
            double x,
            double y,
            double z,
            double width,
            double height,
            double depth,
            double angleRadians,
            Color baseColor
    ) {
    }

    private record Dimensions(double width, double height, double depth) {
    }

    private record MuscleDimensions(
            Dimensions head,
            Dimensions neck,
            Dimensions pelvis,
            Dimensions chest,
            Dimensions absUpper,
            Dimensions absMid,
            Dimensions absLow,
            Dimensions back,
            Dimensions lat,
            Dimensions deltoid,
            Dimensions upperArm,
            Dimensions triceps,
            Dimensions forearm,
            Dimensions wristTaper,
            Dimensions hand,
            Dimensions thigh,
            Dimensions calf,
            Dimensions foot
    ) {
    }

    private record BodyPartAnchors(
            double chestX,
            double chestY,
            double backX,
            double backY,
            double latX,
            double latY,
            double absUpperY,
            double absMidY,
            double absLowY,
            double pelvisY,
            double pelvisWidth,
            double pelvisHeight,
            double shoulderX,
            double shoulderY,
            double elbowY,
            double wristY,
            double deltoidX,
            double deltoidY,
            double tricepsX,
            double upperArmX,
            double upperArmY,
            double forearmX,
            double forearmY,
            double wristTaperX,
            double wristTaperY,
            double handX,
            double handY,
            double thighX,
            double thighY,
            double calfX,
            double calfY,
            double footX,
            double footY,
            double neckY,
            double headY,
            double torsoHalfW
    ) {
    }

    private record LaidOutPrimitive(
            BodyPrimitive primitive,
            RenderPartState state,
            double x,
            double y,
            double z,
            double width,
            double height,
            double depth,
            double angleRadians
    ) {
    }

    private record ProjectedPrimitive(
            LaidOutPrimitive primitive,
            double x,
            double y,
            double depth,
            double width,
            double height,
            double angleRadians
    ) {
    }

    private record MaskSample(double alpha, double nx, double ny) {
        static MaskSample empty() {
            return new MaskSample(0.0, 0.0, 0.0);
        }
    }

    private record RenderPartState(
            Scale3 scale,
            MaterialVisualState material,
            double definition
    ) {
        static RenderPartState neutral() {
            return new RenderPartState(
                    Scale3.identity(),
                    new MaterialVisualState(0.0, 0.7, 0.15, 0.0, 0.0),
                    0.0
            );
        }

        static RenderPartState lerp(MuscleVisualState start, MuscleVisualState target, double amount) {
            return new RenderPartState(
                    new Scale3(
                            lerp(start.localScale().x(), target.localScale().x(), amount),
                            lerp(start.localScale().y(), target.localScale().y(), amount),
                            lerp(start.localScale().z(), target.localScale().z(), amount)
                    ),
                    new MaterialVisualState(
                            lerp(start.material().normalBlend01(), target.material().normalBlend01(), amount),
                            lerp(start.material().roughness(), target.material().roughness(), amount),
                            lerp(start.material().specular01(), target.material().specular01(), amount),
                            lerp(start.material().vascularity01(), target.material().vascularity01(), amount),
                            lerp(start.material().pump01(), target.material().pump01(), amount)
                    ),
                    lerp(start.definitionMorphWeight(), target.definitionMorphWeight(), amount)
            );
        }

        double scaleX() {
            return scale.x() * (1.0 + material.pump01() * 0.14);
        }

        double scaleY() {
            return scale.y();
        }

        double scaleZ() {
            return scale.z() * (1.0 + material.pump01() * 0.14);
        }

        private static double lerp(double from, double to, double amount) {
            return from + (to - from) * clamp01(amount);
        }
    }
}
