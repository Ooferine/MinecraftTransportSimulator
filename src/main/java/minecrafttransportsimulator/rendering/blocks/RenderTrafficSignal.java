package minecrafttransportsimulator.rendering.blocks;

import java.awt.Color;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.blocks.instances.BlockPoleTrafficSignal;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPoleTrafficSignal;
import minecrafttransportsimulator.wrappers.WrapperRender;

public class RenderTrafficSignal extends ARenderTileEntityBase<TileEntityPoleTrafficSignal, BlockPoleTrafficSignal>{
		
	public RenderTrafficSignal(){}
	
	@Override
	public void render(TileEntityPoleTrafficSignal tile, BlockPoleTrafficSignal block, float partialTicks){
		//Start rendering by translating to the position of the TE and rotating it.
		GL11.glPushMatrix();
		GL11.glTranslated(tile.position.x, tile.position.y, tile.position.z);
		GL11.glTranslatef(0.5F, 0F, 0.5F);
		GL11.glRotatef(-block.getRotation(tile.world, tile.position), 0, 1, 0);
	
		//Disable the lighting prior to rendering the lights.
		WrapperRender.setLightmapState(false);
		GL11.glDisable(GL11.GL_LIGHTING);
		float lightBrightness = tile.world.getLightBrightness(tile.position);
		
		//Get the current light state.
		Color lightColor;
		boolean shouldFlash;
		switch(tile.state){
			case GREEN: lightColor = Color.GREEN; shouldFlash = false; break;
			case YELLOW: lightColor = Color.YELLOW; shouldFlash = false; break;
			case RED: lightColor = Color.RED; shouldFlash = false; break;
			case FLASHING_RED: lightColor = Color.RED; shouldFlash = true; break;
			case FLASHING_YELLOW: lightColor = Color.YELLOW; shouldFlash = true; break;
			default: lightColor = null; shouldFlash = false; break;		
		}
		
		//Render based on the current state.
		if(lightColor != null && (!shouldFlash || (shouldFlash && (System.currentTimeMillis()%20000 < 10000)))){
			GL11.glTranslatef(0, lightColor.equals(Color.RED) ? 13F/16F : (lightColor.equals(Color.YELLOW) ? 8F/16F : 3F/16F), 0.225F);
			renderLightedSquare(4F/16F, lightBrightness, lightColor, "textures/rendering/light.png");
		}
		
		//Reset states.
		GL11.glEnable(GL11.GL_LIGHTING);
		WrapperRender.setLightmapState(true);
		GL11.glColor4f(1, 1, 1, 1);
		GL11.glPopMatrix();
	}
}