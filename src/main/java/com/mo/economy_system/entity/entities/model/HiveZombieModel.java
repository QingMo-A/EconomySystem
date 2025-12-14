package com.mo.economy_system.entity.entities.model;

import com.mo.economy_system.entity.entities.HiveZombieEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public class HiveZombieModel <T extends HiveZombieEntity> extends HumanoidModel<T> {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public HiveZombieModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    // 创建模型定义
    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 头部定义（可以调整参数来自定义外观）
        PartDefinition head = partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0)  // 纹理偏移量
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                                new CubeDeformation(0.0F))  // 可以调整变形量来改变形状
                        .texOffs(32, 0)  // 帽子层
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                                new CubeDeformation(0.5F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // 身体定义
        PartDefinition body = partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // 手臂定义 - 可以调整长度或粗细
        PartDefinition rightArm = partdefinition.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        PartDefinition leftArm = partdefinition.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .mirror()  // 镜像，用于对称
                        .addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        // 腿部定义
        PartDefinition rightLeg = partdefinition.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        PartDefinition leftLeg = partdefinition.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .mirror()
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                                new CubeDeformation(0.0F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    // 动画逻辑
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // 僵尸特有的动画逻辑
        boolean isAggressive = entity.isAggressive();
        boolean isAttacking = this.attackTime > 0;

        // 基础行走动画（保持原版僵尸的蹒跚步态）
        float walkSpeed = 0.6662F;
        float walkIntensity = 2.0F;

        // 手臂摆动 - 让丧尸走路时手臂更僵硬地前伸
        this.rightArm.xRot = Mth.cos(limbSwing * walkSpeed + (float)Math.PI) * walkIntensity * limbSwingAmount * 0.7F;
        this.leftArm.xRot = Mth.cos(limbSwing * walkSpeed) * walkIntensity * limbSwingAmount * 0.7F;

        // 腿部摆动 - 更沉重的步伐
        this.rightLeg.xRot = Mth.cos(limbSwing * walkSpeed) * 1.4F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * walkSpeed + (float)Math.PI) * 1.4F * limbSwingAmount;

        if (isAggressive) {
            // 愤怒/追击时的动画
            float chaseSpeed = ageInTicks * 0.3F; // 更快的动画速度

            // 追击时身体前倾
            this.body.xRot = Mth.cos(chaseSpeed) * 0.1F + 0.1F;

            // 追击时手臂更向前伸
            float armExtension = Mth.cos(chaseSpeed) * 0.2F;
            this.rightArm.xRot = -0.5F - armExtension;
            this.leftArm.xRot = -0.5F - armExtension;

            // 追击时头部向前伸，显得更有攻击性
            this.head.xRot = 0.2F + Mth.sin(chaseSpeed) * 0.05F;

            // 追击时步伐更大更快
            this.rightLeg.xRot = Mth.cos(limbSwing * walkSpeed * 1.5F) * 2.0F * limbSwingAmount;
            this.leftLeg.xRot = Mth.cos(limbSwing * walkSpeed * 1.5F + (float)Math.PI) * 2.0F * limbSwingAmount;
        }

        if (isAttacking) {
            // 攻击动画 - 更凶狠的动作
            float attackTime = this.attackTime;

            // 攻击时的身体动作 - 向前猛扑
            this.body.xRot = Mth.sin(attackTime * (float)Math.PI) * 0.5F;
            this.body.y = Mth.sin(attackTime * (float)Math.PI) * 0.2F; // 身体上下移动

            // 攻击时的头部动作 - 向前猛咬
            this.head.xRot = Mth.sin(attackTime * (float)Math.PI) * 0.8F;

            // 猛烈的双臂攻击
            float attackIntensity = Mth.sin(attackTime * (float)Math.PI * 2.0F);
            float attackForce = Mth.sin((1.0F - (1.0F - attackTime) * (1.0F - attackTime)) * (float)Math.PI);

            if (entity.getMainArm() == HumanoidArm.RIGHT) {
                // 右手为主攻手 - 大幅度挥击
                this.rightArm.xRot = -2.0F + Mth.cos(ageInTicks * 0.3F) * 0.2F;
                this.rightArm.xRot += attackForce * 3.0F;
                this.rightArm.zRot = -0.5F + attackIntensity * 0.3F;

                // 左手辅助攻击
                this.leftArm.xRot = -1.5F + Mth.cos(ageInTicks * 0.2F) * 0.1F;
                this.leftArm.xRot += attackForce * 1.5F;
                this.leftArm.zRot = 0.3F;
            } else {
                // 左手为主攻手
                this.leftArm.xRot = -2.0F + Mth.cos(ageInTicks * 0.3F) * 0.2F;
                this.leftArm.xRot += attackForce * 3.0F;
                this.leftArm.zRot = 0.5F - attackIntensity * 0.3F;

                // 右手辅助攻击
                this.rightArm.xRot = -1.5F + Mth.cos(ageInTicks * 0.2F) * 0.1F;
                this.rightArm.xRot += attackForce * 1.5F;
                this.rightArm.zRot = -0.3F;
            }

            // 攻击时腿部蹬地发力
            this.rightLeg.xRot = Mth.cos(attackTime * (float)Math.PI) * 0.8F + 0.5F;
            this.leftLeg.xRot = Mth.cos(attackTime * (float)Math.PI + (float)Math.PI) * 0.8F + 0.5F;

            // 攻击时身体轻微旋转（扭转发力）
            this.body.yRot = attackIntensity * 0.3F;
        } else if (!isAggressive) {
            // 空闲/漫游时的动画
            float idleBob = Mth.cos(ageInTicks * 0.05F) * 0.02F;

            // 轻微的呼吸起伏
            this.body.xRot = idleBob * 0.5F;
            this.head.xRot = idleBob * 0.3F;

            // 手臂轻微晃动
            float idleArmSway = Mth.cos(ageInTicks * 0.1F) * 0.05F;
            this.rightArm.xRot = -0.1F + idleArmSway;
            this.leftArm.xRot = -0.1F + idleArmSway;
        }

        // 头部跟随目标（保持原版逻辑，但添加一些随机晃动）
        this.head.yRot = netHeadYaw * ((float)Math.PI / 180F);

        // 添加头部随机晃动，显得更不自然/丧尸化
        if (!isAttacking) {
            float headJitter = Mth.cos(ageInTicks * 0.3F) * 0.02F;
            this.head.yRot += headJitter;
            this.head.xRot += Mth.sin(ageInTicks * 0.2F) * 0.01F;
        }

        // 让丧尸走路时身体轻微晃动（更明显的蹒跚）
        if (limbSwingAmount > 0.1F) {
            float wobble = Mth.sin(limbSwing * 0.6662F) * 0.3F * limbSwingAmount;
            this.body.yRot = wobble;

            // 身体左右倾斜
            float lean = Mth.cos(limbSwing * 1.333F) * 0.1F * limbSwingAmount;
            this.body.zRot = lean;
        }
    }

    // 渲染方法
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,
                               int packedLight, int packedOverlay, float red,
                               float green, float blue, float alpha) {
        // 渲染所有部件
        head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        rightArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        leftArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
