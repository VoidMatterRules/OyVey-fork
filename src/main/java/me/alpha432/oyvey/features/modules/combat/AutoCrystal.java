package me.alpha432.oyvey.features.modules.combat;

import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.events.PacketEvent;
import me.alpha432.oyvey.event.events.Render3DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.ClickGui;
import me.alpha432.oyvey.features.modules.misc.AutoGG;
import me.alpha432.oyvey.features.setting.Setting;
import me.alpha432.oyvey.util.Timer;
import me.alpha432.oyvey.util.*;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.Explosion;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AutoCrystal
        extends Module {
    private final Timer placeTimer = new Timer();
    private final Timer breakTimer = new Timer();
    private final Timer predictTimer = new Timer();
    private final Timer manualTimer = new Timer();
    private final Setting<Integer> attackFactor = this.register(new Setting<>("PredictDelay", 0, 0, 200));
    private final Setting<Integer> red = this.register(new Setting<>("Red", 0, 0, 255));
    private final Setting<Integer> green = this.register(new Setting<>("Green", 255, 0, 255));
    private final Setting<Integer> blue = this.register(new Setting<>("Blue", 0, 0, 255));
    private final Setting<Integer> alpha = this.register(new Setting<>("Alpha", 255, 0, 255));
    private final Setting<Integer> boxAlpha = this.register(new Setting<>("BoxAlpha", 125, 0, 255));
    private final Setting<Float> lineWidth = this.register(new Setting<>("LineWidth", 1.0f, 0.1f, 5.0f));
    public Setting<Boolean> place = this.register(new Setting<>("Place", true));
    public Setting<Float> placeDelay = this.register(new Setting<>("PlaceDelay", 4.0f, 0.0f, 300.0f));
    public Setting<Float> placeRange = this.register(new Setting<>("PlaceRange", 4.0f, 0.1f, 7.0f));
    public Setting<Float> placeWallRange = this.register(new Setting<>("PlaceWallRange", 4.0f, 0.1f, 7.0f));
    public Setting<Boolean> explode = this.register(new Setting<>("Break", true));
    public Setting<Boolean> packetBreak = this.register(new Setting<>("PacketBreak", true));
    public Setting<Boolean> predicts = this.register(new Setting<>("Predict", true));
    public Setting<Boolean> rotate = this.register(new Setting<>("Rotate", true));
    public Setting<Float> breakDelay = this.register(new Setting<>("BreakDelay", 4.0f, 0.0f, 300.0f));
    public Setting<Float> breakRange = this.register(new Setting<>("BreakRange", 4.0f, 0.1f, 7.0f));
    public Setting<Float> breakWallRange = this.register(new Setting<>("BreakWallRange", 4.0f, 0.1f, 7.0f));
    public Setting<Boolean> opPlace = this.register(new Setting<>("1.13 Place", true));
    public Setting<Boolean> suicide = this.register(new Setting<>("AntiSuicide", true));
    public Setting<Boolean> autoswitch = this.register(new Setting<>("AutoSwitch", true));
    public Setting<Boolean> ignoreUseAmount = this.register(new Setting<>("IgnoreUseAmount", true));
    public Setting<Integer> wasteAmount = this.register(new Setting<>("UseAmount", 4, 1, 5));
    public Setting<Boolean> facePlaceSword = this.register(new Setting<>("FacePlaceSword", true));
    public Setting<Float> targetRange = this.register(new Setting<>("TargetRange", 4.0f, 1.0f, 12.0f));
    public Setting<Float> minDamage = this.register(new Setting<>("MinDamage", 4.0f, 0.1f, 20.0f));
    public Setting<Float> facePlace = this.register(new Setting<>("FacePlaceHP", 4.0f, 0.0f, 36.0f));
    public Setting<Float> breakMaxSelfDamage = this.register(new Setting<>("BreakMaxSelf", 4.0f, 0.1f, 12.0f));
    public Setting<Float> breakMinDmg = this.register(new Setting<>("BreakMinDmg", 4.0f, 0.1f, 7.0f));
    public Setting<Float> minArmor = this.register(new Setting<>("MinArmor", 4.0f, 0.1f, 80.0f));
    public Setting<SwingMode> swingMode = this.register(new Setting<>("Swing", SwingMode.MainHand));
    public Setting<Boolean> render = this.register(new Setting<>("Render", true));
    public Setting<Boolean> renderDmg = this.register(new Setting<>("RenderDmg", true));
    public Setting<Boolean> box = this.register(new Setting<>("Box", true));
    public Setting<Boolean> outline = this.register(new Setting<>("Outline", true));
    private final Setting<Integer> cRed = this.register(new Setting<Object>("OL-Red", 0, 0, 255, v -> this.outline.getValue()));
    private final Setting<Integer> cGreen = this.register(new Setting<Object>("OL-Green", 0, 0, 255, v -> this.outline.getValue()));
    private final Setting<Integer> cBlue = this.register(new Setting<Object>("OL-Blue", 255, 0, 255, v -> this.outline.getValue()));
    private final Setting<Integer> cAlpha = this.register(new Setting<Object>("OL-Alpha", 255, 0, 255, v -> this.outline.getValue()));
    EntityEnderCrystal crystal;
    private EntityLivingBase target;
    private BlockPos pos;
    private int hotBarSlot;
    private boolean armor;
    private boolean armorTarget;
    private float yaw = 0.0f;
    private EntityLivingBase realTarget;
    private float pitch = 0.0f;
    private boolean rotating = false;

    public AutoCrystal() {
        super("AutoCrystal", "NiggaHack ac best ac", Module.Category.COMBAT, true, false, false);
    }

    public static List<BlockPos> getSphere(BlockPos loc, float r, int h, boolean hollow, boolean sphere, int plus_y) {
        ArrayList<BlockPos> circleblocks = new ArrayList<>();
        int cx = loc.getX();
        int cy = loc.getY();
        int cz = loc.getZ();
        int x = cx - (int) r;
        while ((float) x <= (float) cx + r) {
            int z = cz - (int) r;
            while ((float) z <= (float) cz + r) {
                int y = sphere ? cy - (int) r : cy;
                while (true) {
                    float f;
                    f = sphere ? (float) cy + r : (float) (cy + h);
                    if (!((float) y < f)) break;
                    double dist = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0);
                    if (!(!(dist < (double) (r * r)) || hollow && dist < (double) ((r - 1.0f) * (r - 1.0f)))) {
                        BlockPos l = new BlockPos(x, y + plus_y, z);
                        circleblocks.add(l);
                    }
                    ++y;
                }
                ++z;
            }
            ++x;
        }
        return circleblocks;
    }

    @SubscribeEvent
    public void onPacketSend(PacketEvent.Send event) {
        if (event.getStage() == 0 && this.rotate.getValue() && this.rotating && event.getPacket() instanceof CPacketPlayer) {
            CPacketPlayer packet = event.getPacket();
            packet.yaw = this.yaw;
            packet.pitch = this.pitch;
            this.rotating = false;
        }
    }

    private void rotateTo(Entity entity) {
        if (this.rotate.getValue()) {
            float[] angle = MathUtil.calcAngle(mc.player.getPositionEyes(mc.getRenderPartialTicks()), entity.getPositionVector());
            this.yaw = angle[0];
            this.pitch = angle[1];
            this.rotating = true;
        }
    }

    private void rotateToPos(BlockPos pos) {
        if (this.rotate.getValue()) {
            float[] angle = MathUtil.calcAngle(mc.player.getPositionEyes(mc.getRenderPartialTicks()), new Vec3d((float) pos.getX() + 0.5f, (float) pos.getY() - 0.5f, (float) pos.getZ() + 0.5f));
            this.yaw = angle[0];
            this.pitch = angle[1];
            this.rotating = true;
        }
    }

    @Override
    public void onEnable() {
        this.placeTimer.reset();
        this.breakTimer.reset();
        this.hotBarSlot = -1;
        this.pos = null;
        this.crystal = null;
        this.target = null;
        this.realTarget = null;
        this.armor = false;
        this.armorTarget = false;
    }

    @Override
    public void onDisable() {
        this.rotating = false;
    }

    @Override
    public void onTick() {
        this.onCrystal();
    }

    @Override
    public String getDisplayInfo() {
        if (this.realTarget != null) {
            return this.realTarget.getName();
        }
        return null;
    }

    public void onCrystal() {
        if (mc.world == null || mc.player == null) {
            return;
        }
        this.realTarget = null;
        this.manualBreaker();
        int crystalCount = 0;
        if (!this.ignoreUseAmount.getValue()) {
            for (Entity crystal : mc.world.loadedEntityList) {
                if (!(crystal instanceof EntityEnderCrystal) || !this.IsValidCrystal(crystal)) continue;
                boolean count = false;
                double damage = this.calculateDamage((double) this.target.getPosition().getX() + 0.5, (double) this.target.getPosition().getY() + 1.0, (double) this.target.getPosition().getZ() + 0.5, this.target);
                if (damage >= (double) this.minDamage.getValue()) {
                    count = true;
                }
                if (!count) continue;
                ++crystalCount;
            }
        }
        this.hotBarSlot = -1;
        if (mc.player.getHeldItemOffhand().getItem() != Items.END_CRYSTAL) {
            int crystalSlot;
            crystalSlot = mc.player.getHeldItemMainhand().getItem() == Items.END_CRYSTAL ? mc.player.inventory.currentItem : -1;
            if (crystalSlot == -1) {
                for (int l = 0; l < 9; ++l) {
                    if (mc.player.inventory.getStackInSlot(l).getItem() != Items.END_CRYSTAL) continue;
                    crystalSlot = l;
                    this.hotBarSlot = l;
                    break;
                }
            }
            if (crystalSlot == -1) {
                this.pos = null;
                this.target = null;
                return;
            }
        }
        if (mc.player.getHeldItemOffhand().getItem() == Items.GOLDEN_APPLE && mc.player.getHeldItemMainhand().getItem() != Items.END_CRYSTAL) {
            this.pos = null;
            this.target = null;
            return;
        }
        if (this.target == null) {
            this.target = this.getTarget();
        }
        if (this.target == null) {
            this.crystal = null;
            return;
        }
        if (this.target.getDistance(mc.player) > 12.0f) {
            this.crystal = null;
            this.target = null;
        }
        this.crystal = mc.world.loadedEntityList.stream().filter(this::IsValidCrystal).map(p_Entity -> (EntityEnderCrystal) p_Entity).min(Comparator.comparing(p_Entity -> this.target.getDistance(p_Entity))).orElse(null);
        if (this.crystal != null && this.explode.getValue() && this.breakTimer.passedMs(this.breakDelay.getValue().longValue())) {
            this.breakTimer.reset();
            if (this.packetBreak.getValue()) {
                this.rotateTo(this.crystal);
                mc.player.connection.sendPacket(new CPacketUseEntity(this.crystal));
            } else {
                this.rotateTo(this.crystal);
                mc.playerController.attackEntity(mc.player, this.crystal);
            }
            if (this.swingMode.getValue() == SwingMode.MainHand) {
                mc.player.swingArm(EnumHand.MAIN_HAND);
            } else if (this.swingMode.getValue() == SwingMode.OffHand) {
                mc.player.swingArm(EnumHand.OFF_HAND);
            }
        }
        if (this.placeTimer.passedMs(this.placeDelay.getValue().longValue()) && this.place.getValue()) {
            this.placeTimer.reset();
            double damage = 0.5;
            for (BlockPos blockPos : this.placePostions(this.placeRange.getValue())) {
                if (blockPos == null) continue;
                if (!canSeePos(blockPos) && mc.player.getDistance(blockPos.getX(), blockPos.getY(), blockPos.getZ()) > (double) placeWallRange.getValue()) {
                    continue;
                }
                double selfDmg;
                if (this.target == null || !mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(blockPos)).isEmpty() || (this.target.getDistance(blockPos.getX(), blockPos.getY(), blockPos.getZ())) > (double) this.targetRange.getValue() || this.target.isDead || this.target.getHealth() + this.target.getAbsorptionAmount() <= 0.0f)
                    continue;
                double targetDmg = this.calculateDamage((double) blockPos.getX() + 0.5, (double) blockPos.getY() + 1.0, (double) blockPos.getZ() + 0.5, this.target);
                this.armor = false;
                for (ItemStack is : this.target.getArmorInventoryList()) {
                    float green = ((float) is.getMaxDamage() - (float) is.getItemDamage()) / (float) is.getMaxDamage();
                    float red = 1.0f - green;
                    int dmg = 100 - (int) (red * 100.0f);
                    if (!((float) dmg <= this.minArmor.getValue())) continue;
                    this.armor = true;
                }
                if (targetDmg < (double) this.minDamage.getValue() && (this.facePlaceSword.getValue() != false ? this.target.getAbsorptionAmount() + this.target.getHealth() > this.facePlace.getValue() : mc.player.getHeldItemMainhand().getItem() instanceof ItemSword || this.target.getAbsorptionAmount() + this.target.getHealth() > this.facePlace.getValue()) && (this.facePlaceSword.getValue() != false ? !this.armor : mc.player.getHeldItemMainhand().getItem() instanceof ItemSword || !this.armor) || (selfDmg = this.calculateDamage((double) blockPos.getX() + 0.5, (double) blockPos.getY() + 1.0, (double) blockPos.getZ() + 0.5, mc.player)) + (this.suicide.getValue() != false ? 2.0 : 0.5) >= (double) (mc.player.getHealth() + mc.player.getAbsorptionAmount()) && selfDmg >= targetDmg && targetDmg < (double) (this.target.getHealth() + this.target.getAbsorptionAmount()) || !(damage < targetDmg))
                    continue;
                this.pos = blockPos;
                damage = targetDmg;
            }
            if (damage == 0.5) {
                this.pos = null;
                this.target = null;
                this.realTarget = null;
                return;
            }
            this.realTarget = this.target;
            if (AutoGG.getINSTANCE().isOn()) {
                AutoGG autoGG = (AutoGG) OyVey.moduleManager.getModuleByName("AutoGG");
                autoGG.addTargetedPlayer(this.target.getName());
            }
            if (this.hotBarSlot != -1 && this.autoswitch.getValue() && !mc.player.isPotionActive(MobEffects.WEAKNESS)) {
                mc.player.inventory.currentItem = this.hotBarSlot;
            }
            if (!this.ignoreUseAmount.getValue()) {
                int crystalLimit = this.wasteAmount.getValue();
                if (crystalCount >= crystalLimit) {
                    return;
                }
                if (damage < (double) this.minDamage.getValue()) {
                    crystalLimit = 1;
                }
                if (crystalCount < crystalLimit && this.pos != null) {
                    this.rotateToPos(this.pos);
                    mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(this.pos, EnumFacing.UP, mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, 0.0f, 0.0f, 0.0f));
                }
            } else if (this.pos != null) {
                this.rotateToPos(this.pos);
                mc.player.connection.sendPacket(new CPacketPlayerTryUseItemOnBlock(this.pos, EnumFacing.UP, mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, 0.0f, 0.0f, 0.0f));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public void onPacketReceive(PacketEvent.Receive event) {
        SPacketSpawnObject packet;
        if (event.getPacket() instanceof SPacketSpawnObject && (packet = event.getPacket()).getType() == 51 && this.predicts.getValue() && this.predictTimer.passedMs(this.attackFactor.getValue().longValue()) && this.predicts.getValue() && this.explode.getValue() && this.packetBreak.getValue() && this.target != null) {
            if (!this.isPredicting(packet)) {
                return;
            }
            CPacketUseEntity predict = new CPacketUseEntity();
            predict.entityId = packet.getEntityID();
            predict.action = CPacketUseEntity.Action.ATTACK;
            mc.player.connection.sendPacket(predict);
        }
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (this.pos != null && this.render.getValue() && this.target != null) {
            RenderUtil.drawBoxESP(this.pos, ClickGui.getInstance().rainbow.getValue() != false ? ColorUtil.rainbow(ClickGui.getInstance().rainbowHue.getValue()) : new Color(this.red.getValue(), this.green.getValue(), this.blue.getValue(), this.alpha.getValue()), this.outline.getValue(), ClickGui.getInstance().rainbow.getValue() != false ? ColorUtil.rainbow(ClickGui.getInstance().rainbowHue.getValue()) : new Color(this.cRed.getValue(), this.cGreen.getValue(), this.cBlue.getValue(), this.cAlpha.getValue()), this.lineWidth.getValue(), this.outline.getValue(), this.box.getValue(), this.boxAlpha.getValue(), true);
            if (this.renderDmg.getValue()) {
                double renderDamage = this.calculateDamage((double) this.pos.getX() + 0.5, (double) this.pos.getY() + 1.0, (double) this.pos.getZ() + 0.5, this.target);
                RenderUtil.drawText(this.pos, (Math.floor(renderDamage) == renderDamage ? Integer.valueOf((int) renderDamage) : String.format("%.1f", renderDamage)) + "");
            }
        }
    }

    private boolean isPredicting(SPacketSpawnObject packet) {
        BlockPos packPos = new BlockPos(packet.getX(), packet.getY(), packet.getZ());
        if (mc.player.getDistance(packet.getX(), packet.getY(), packet.getZ()) > (double) this.breakRange.getValue()) {
            return false;
        }
        if (!this.canSeePos(packPos) && mc.player.getDistance(packet.getX(), packet.getY(), packet.getZ()) > (double) this.breakWallRange.getValue()) {
            return false;
        }
        double targetDmg = this.calculateDamage(packet.getX() + 0.5, packet.getY() + 1.0, packet.getZ() + 0.5, this.target);
        if (EntityUtil.isInHole(mc.player) && targetDmg >= 1.0) {
            return true;
        }
        double selfDmg = this.calculateDamage(packet.getX() + 0.5, packet.getY() + 1.0, packet.getZ() + 0.5, mc.player);
        double d = this.suicide.getValue() != false ? 2.0 : 0.5;
        if (selfDmg + d < (double) (mc.player.getHealth() + mc.player.getAbsorptionAmount()) && targetDmg >= (double) (this.target.getAbsorptionAmount() + this.target.getHealth())) {
            return true;
        }
        this.armorTarget = false;
        for (ItemStack is : this.target.getArmorInventoryList()) {
            float green = ((float) is.getMaxDamage() - (float) is.getItemDamage()) / (float) is.getMaxDamage();
            float red = 1.0f - green;
            int dmg = 100 - (int) (red * 100.0f);
            if (!((float) dmg <= this.minArmor.getValue())) continue;
            this.armorTarget = true;
        }
        if (targetDmg >= (double) this.breakMinDmg.getValue() && selfDmg <= (double) this.breakMaxSelfDamage.getValue()) {
            return true;
        }
        return EntityUtil.isInHole(this.target) && this.target.getHealth() + this.target.getAbsorptionAmount() <= this.facePlace.getValue();
    }

    private boolean IsValidCrystal(Entity p_Entity) {
        if (p_Entity == null) {
            return false;
        }
        if (!(p_Entity instanceof EntityEnderCrystal)) {
            return false;
        }
        if (this.target == null) {
            return false;
        }
        if (p_Entity.getDistance(mc.player) > this.breakRange.getValue()) {
            return false;
        }
        if (!mc.player.canEntityBeSeen(p_Entity) && p_Entity.getDistance(mc.player) > this.breakWallRange.getValue()) {
            return false;
        }
        if (this.target.isDead || this.target.getHealth() + this.target.getAbsorptionAmount() <= 0.0f) {
            return false;
        }
        double targetDmg = this.calculateDamage((double) p_Entity.getPosition().getX() + 0.5, (double) p_Entity.getPosition().getY() + 1.0, (double) p_Entity.getPosition().getZ() + 0.5, this.target);
        if (EntityUtil.isInHole(mc.player) && targetDmg >= 1.0) {
            return true;
        }
        double selfDmg = this.calculateDamage((double) p_Entity.getPosition().getX() + 0.5, (double) p_Entity.getPosition().getY() + 1.0, (double) p_Entity.getPosition().getZ() + 0.5, mc.player);
        double d = this.suicide.getValue() != false ? 2.0 : 0.5;
        if (selfDmg + d < (double) (mc.player.getHealth() + mc.player.getAbsorptionAmount()) && targetDmg >= (double) (this.target.getAbsorptionAmount() + this.target.getHealth())) {
            return true;
        }
        this.armorTarget = false;
        for (ItemStack is : this.target.getArmorInventoryList()) {
            float green = ((float) is.getMaxDamage() - (float) is.getItemDamage()) / (float) is.getMaxDamage();
            float red = 1.0f - green;
            int dmg = 100 - (int) (red * 100.0f);
            if (!((float) dmg <= this.minArmor.getValue())) continue;
            this.armorTarget = true;
        }
        if (targetDmg >= (double) this.breakMinDmg.getValue() && selfDmg <= (double) this.breakMaxSelfDamage.getValue()) {
            return true;
        }
        return EntityUtil.isInHole(this.target) && this.target.getHealth() + this.target.getAbsorptionAmount() <= this.facePlace.getValue();
    }

    EntityPlayer getTarget() {
        EntityPlayer targetPlayer = null;
        float targetDmg = 0.0f;
        for (EntityPlayer entity : mc.world.playerEntities) {
            if (mc.player == null || mc.player.isDead || entity.isDead || entity == mc.player || OyVey.friendManager.isFriend(entity.getName()) || entity.getDistance(mc.player) > 12.0f)
                continue;
            this.armorTarget = false;
            for (ItemStack is : entity.getArmorInventoryList()) {
                float green = ((float) is.getMaxDamage() - (float) is.getItemDamage()) / (float) is.getMaxDamage();
                float red = 1.0f - green;
                int dmg = 100 - (int) (red * 100.0f);
                if (!((float) dmg <= this.minArmor.getValue())) continue;
                this.armorTarget = true;
            }
            if (EntityUtil.isInHole(entity) && entity.getAbsorptionAmount() + entity.getHealth() > this.facePlace.getValue() && !this.armorTarget && this.minDamage.getValue() > 2.2f)
                continue;
            if (targetPlayer == null) {
                targetPlayer = entity;
                continue;
            }
            float currentDmg = 0.0f;
            int range = placeRange.getValue().intValue();
            for (int x = -range; x < range; x++) {
                for (int y = -range; y < range; y++) {
                    for (int z = -range; z < range; z++) {
                        currentDmg = calculateDamage(x, y, z, entity);
                    }
                }
            }
            if (currentDmg <= targetDmg) continue;
            targetPlayer = entity;
            targetDmg = currentDmg;
        }
        return targetPlayer;
    }

    private void manualBreaker() {
        RayTraceResult result;
        if (this.manualTimer.passedMs(200L) && mc.gameSettings.keyBindUseItem.isKeyDown() && mc.player.getHeldItemOffhand().getItem() != Items.GOLDEN_APPLE && mc.player.inventory.getCurrentItem().getItem() != Items.GOLDEN_APPLE && mc.player.inventory.getCurrentItem().getItem() != Items.BOW && mc.player.inventory.getCurrentItem().getItem() != Items.EXPERIENCE_BOTTLE && (result = mc.objectMouseOver) != null) {
            if (result.typeOfHit.equals(RayTraceResult.Type.ENTITY)) {
                Entity entity = result.entityHit;
                if (entity instanceof EntityEnderCrystal) {
                    if (this.packetBreak.getValue()) {
                        mc.player.connection.sendPacket(new CPacketUseEntity(entity));
                    } else {
                        mc.playerController.attackEntity(mc.player, entity);
                    }
                    this.manualTimer.reset();
                }
            } else if (result.typeOfHit.equals(RayTraceResult.Type.BLOCK)) {
                BlockPos mousePos = new BlockPos(mc.objectMouseOver.getBlockPos().getX(), (double) mc.objectMouseOver.getBlockPos().getY() + 1.0, mc.objectMouseOver.getBlockPos().getZ());
                for (Entity target : mc.world.getEntitiesWithinAABBExcludingEntity(null, new AxisAlignedBB(mousePos))) {
                    if (!(target instanceof EntityEnderCrystal)) continue;
                    if (this.packetBreak.getValue()) {
                        mc.player.connection.sendPacket(new CPacketUseEntity(target));
                    } else {
                        mc.playerController.attackEntity(mc.player, target);
                    }
                    this.manualTimer.reset();
                }
            }
        }
    }

    private boolean canSeePos(BlockPos pos) {
        return mc.world.rayTraceBlocks(new Vec3d(mc.player.posX, mc.player.posY + (double) mc.player.getEyeHeight(), mc.player.posZ), new Vec3d(pos.getX(), pos.getY(), pos.getZ()), false, true, false) == null;
    }

    private NonNullList<BlockPos> placePostions(float placeRange) {
        NonNullList positions = NonNullList.create();
        positions.addAll(AutoCrystal.getSphere(new BlockPos(Math.floor(mc.player.posX), Math.floor(mc.player.posY + 1), Math.floor(mc.player.posZ)), placeRange, (int) placeRange, false, true, 0).stream().filter(pos -> this.canPlaceCrystal(pos, true)).collect(Collectors.toList()));
        return positions;
    }

    private boolean canPlaceCrystal(BlockPos blockPos, boolean specialEntityCheck) {
        BlockPos boost = blockPos.add(0, 1, 0);
        BlockPos boost2 = blockPos.add(0, 2, 0);
        try {
            if (!this.opPlace.getValue()) {
                if (mc.world.getBlockState(blockPos).getBlock() != Blocks.BEDROCK && mc.world.getBlockState(blockPos).getBlock() != Blocks.OBSIDIAN) {
                    return false;
                }
                if (mc.world.getBlockState(boost).getBlock() != Blocks.AIR || mc.world.getBlockState(boost2).getBlock() != Blocks.AIR) {
                    return false;
                }
                if (!specialEntityCheck) {
                    return mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost)).isEmpty() && mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost2)).isEmpty();
                }
                for (Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost))) {
                    if (entity instanceof EntityEnderCrystal) continue;
                    return false;
                }
                for (Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost2))) {
                    if (entity instanceof EntityEnderCrystal) continue;
                    return false;
                }
            } else {
                if (mc.world.getBlockState(blockPos).getBlock() != Blocks.BEDROCK && mc.world.getBlockState(blockPos).getBlock() != Blocks.OBSIDIAN) {
                    return false;
                }
                if (mc.world.getBlockState(boost).getBlock() != Blocks.AIR) {
                    return false;
                }
                if (!specialEntityCheck) {
                    return mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost)).isEmpty();
                }
                for (Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(boost))) {
                    if (entity instanceof EntityEnderCrystal) continue;
                    return false;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    private float calculateDamage(double posX, double posY, double posZ, Entity entity) {
        double distancedsize = entity.getDistance(posX, posY, posZ) / 12.0;
        Vec3d vec3d = new Vec3d(posX, posY, posZ);
        double blockDensity = 0.0;
        try {
            blockDensity = entity.world.getBlockDensity(vec3d, entity.getEntityBoundingBox());
        } catch (Exception exception) {
            // empty catch block
        }
        double v = (1.0 - distancedsize) * blockDensity;
        float damage = (int) ((v * v + v) / 2.0 * 7.0 * 12.0 + 1.0);
        double finald = 1.0;
        if (entity instanceof EntityLivingBase) {
            finald = DamageUtil.getBlastReduction((EntityLivingBase) entity, this.getDamageMultiplied(damage), new Explosion(mc.world, null, posX, posY, posZ, 6.0f, false, true));
        }
        return (float) finald;
    }

    private float getDamageMultiplied(float damage) {
        int diff = mc.world.getDifficulty().getId();
        return damage * (diff == 0 ? 0.0f : (diff == 2 ? 1.0f : (diff == 1 ? 0.5f : 1.5f)));
    }

    public enum SwingMode {
        MainHand,
        OffHand,
        None

    }
}

