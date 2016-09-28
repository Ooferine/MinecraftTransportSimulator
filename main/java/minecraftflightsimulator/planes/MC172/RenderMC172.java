package minecraftflightsimulator.planes.MC172;

import java.awt.Color;

import minecraftflightsimulator.entities.core.EntityPlane;
import minecraftflightsimulator.modelrenders.RenderPlane;
import minecraftflightsimulator.utilities.InstrumentHelper;
import minecraftflightsimulator.utilities.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class RenderMC172 extends RenderPlane{
	private static final ModelMC172 model = new ModelMC172();
	private static final ResourceLocation[] planeTextures = getPlaneTextures();

	public RenderMC172(RenderManager manager){
		super(manager);
	}
	
	@Override
	protected void renderPlane(EntityPlane plane){
    	RenderHelper.bindTexture(planeTextures[plane.textureOptions > 5 ? 0 : plane.textureOptions]);
        model.renderPlane();
        model.renderAilerons(plane.aileronAngle/10F * 0.017453292F);
        model.renderElevators(plane.elevatorAngle/10F * 0.017453292F);
        model.renderRudder(plane.rudderAngle/10F * 0.017453292F);
        model.renderFlaps(plane.flapAngle/10F * 0.017453292F);
	}

	@Override
	protected void renderStrobeLightCovers(EntityPlane plane){
		//Landing light case.
		GL11.glPushMatrix();
		GL11.glTranslatef(0, -0.375F, 2.157F);
		RenderHelper.bindTexture(windowTexture);
		RenderHelper.renderSquare(-0.125, 0.125, 0, 0.25, 0.001, 0.001, false);
		GL11.glPopMatrix();
		
		drawStrobeLightCover(4.5F, 1.625F, 0.8125F, 90);
		drawStrobeLightCover(-4.5F, 1.625F, 0.8125F, -90);
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0F, 1.875F, -4.25F);
		GL11.glRotatef(180 + plane.rudderAngle/10F, 0, 1, 0);
		drawStrobeLightCover(0, 0, 0.5F, 0);
		GL11.glPopMatrix();
	}
	
	@Override
	protected void renderStrobeLights(EntityPlane plane){
		drawStrobeLight(plane, 4.5F, 1.625F, 0.8125F, 90, 1, 0, 0);
		drawStrobeLight(plane, -4.5F, 1.625F, 0.8125F, -90, 0, 1, 0);
		
		GL11.glPushMatrix();
		GL11.glTranslatef(0F, 1.875F, -4.25F);
		GL11.glRotatef(180 + plane.rudderAngle/10F, 0, 1, 0);
		drawStrobeLight(plane, 0, 0, 0.5F, 0, 1, 1, 1);
		GL11.glPopMatrix();
	}
	
	@Override
	public void renderLights(EntityPlane plane){
		if(plane.lightsOn && plane.auxLightsOn  && plane.electricPower > 2){
			GL11.glPushMatrix();
			GL11.glTranslatef(0, -0.375F, 2.157F);
			GL11.glDisable(GL11.GL_TEXTURE_2D);
			GL11.glDisable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_BLEND);
			GL11.glColor4f(1, 1, 1, (float) plane.electricPower/12F);
			RenderHelper.renderSquare(-0.125, 0.125, 0, 0.25, 0, 0, true);
			GL11.glDisable(GL11.GL_BLEND);
			GL11.glPopMatrix();
			
			GL11.glPushMatrix();
			GL11.glTranslatef(0, -0.15F, 2.1F);
			GL11.glRotatef(35, 1, 0, 0);
			RenderHelper.drawLightBeam(plane, 7, 10, 20);
			GL11.glPopMatrix();
		}
	}
	
	@Override
	protected void renderWindows(EntityPlane plane){
		RenderHelper.bindTexture(windowTexture);
		RenderHelper.renderQuad(-0.75, -0.75, 0.75, 0.75, 1.625, 0.625, 0.625, 1.625, 0.875, 1.75, 1.75, 0.875, true);
		RenderHelper.renderTriangle(-0.75, -0.75, -0.75, 1.625, 0.625, 0.625, 0.875, 0.875, 1.75, true);
		RenderHelper.renderTriangle(0.75, 0.75, 0.75, 1.625, 0.625, 0.625, 0.875, 0.875, 1.75, true);
		RenderHelper.renderSquare(0.85, 0.85, 0.625, 1.625, -0.25, 0.625, true);
		RenderHelper.renderSquare(-0.85, -0.85, 0.625, 1.625, -0.25, 0.625, true);
		RenderHelper.renderTriangle(-0.85, -0.85, -0.7, 1.6, 0.625, 0.625, -0.5, -0.5, -1.95, true);
		RenderHelper.renderTriangle(0.85, 0.85, 0.7, 1.6, 0.625, 0.625, -0.5, -0.5, -1.95, true);
		RenderHelper.renderQuad(-0.8, -0.525, 0.525, 0.8, 1.625, 0.625, 0.625, 1.625, -0.5, -2.1, -2.1, -0.5, true);
	}

	@Override
	protected void renderConsole(EntityPlane plane){
		GL11.glPushMatrix();
		GL11.glTranslatef(0.92F, 0.35F, 0.715F);
		GL11.glRotatef(180, 0, 0, 1);
		GL11.glScalef(0.00390625F*1.5F, 0.00390625F*1.5F, 0.00390625F*1.5F);
		for(byte i=0; i<plane.instrumentList.size(); ++i){
			if(plane.instrumentList.get(i) != null){
				int type = plane.instrumentList.get(i).getItemDamage();
				if(i==0 || i==5){
					GL11.glPushMatrix();
					GL11.glRotatef(-90, 0, 1, 0);
					GL11.glTranslatef(-80, 0, -30);
					GL11.glScalef(0.75F, 0.75F, 0.75F);
					InstrumentHelper.drawFlyableInstrument(plane, 72 + (i%5)*62, i<5 ? -10 : 52, type, false);
					GL11.glPopMatrix();
				}else if(i==4 || i==9){
					GL11.glPushMatrix();
					GL11.glScalef(0.75F, 0.75F, 0.75F);
					InstrumentHelper.drawFlyableInstrument(plane, 72 + (i%5)*62, i<5 ? -10 : 52, type, false);
					GL11.glPopMatrix();
				}else{
					InstrumentHelper.drawFlyableInstrument(plane, (i%5)*62, i<5 ? 0 : 62, type, false);
				}
			}
		}
		InstrumentHelper.drawFlyableInstrument(plane, 272, -5, 15, false);
		InstrumentHelper.drawFlyableInstrument(plane, 272, 60, 16, false);
		InstrumentHelper.drawFlyableInstrument(plane, 232, 80, 17, false);
		GL11.glPopMatrix();
	}

	@Override
	protected void renderMarkings(EntityPlane plane){
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glRotatef(180, 1, 0, 0);
		GL11.glRotatef(105, 0, 1, 0);
		RenderHelper.drawScaledStringAt(plane.displayName, -2.8F, -0.35F, -1.36F, 1F/32F, Color.lightGray);
		GL11.glRotatef(150, 0, 1, 0);
		RenderHelper.drawScaledStringAt(plane.displayName, 2.8F, -0.35F, -1.36F, 1F/32F, Color.lightGray);
		GL11.glRotatef(105, 0, 1, 0);
		GL11.glRotatef(-180, 1, 0, 0);
		GL11.glEnable(GL11.GL_LIGHTING);
	}
	
	private static ResourceLocation[] getPlaneTextures(){
		ResourceLocation[] texArray = new ResourceLocation[6];
		int texIndex = 0;
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_oak.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_spruce.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_spruce.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_jungle.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_acacia.png");
		texArray[texIndex++] = new ResourceLocation("textures/blocks/planks_big_oak.png");
		return texArray;
	}
}
