package minecrafttransportsimulator.blocks.tileentities.instances;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ITileEntityTickable;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.instances.RenderDecor;

/**Decor tile entity.  Contains the definition so we know how
 * to render this in the TESR call, as well as if we need to do
 * crafting operations if we are a crafting decor type.
 *
 * @author don_bruce
 */
public class TileEntityDecor extends ATileEntityBase<JSONDecor> implements ITileEntityTickable{
	public final BoundingBox[] boundingBoxes = new BoundingBox[4];
	
	private static RenderDecor renderer;
	
	private boolean converted;
	
	public TileEntityDecor(WrapperWorld world, Point3d position, WrapperNBT data){
		super(world, position, data);
		//Add a bounding box for each rotation.
		this.boundingBoxes[0] = new BoundingBox(new Point3d(), definition.decor.width/2D, definition.decor.height/2D, definition.decor.depth/2D);
		this.boundingBoxes[1] = new BoundingBox(new Point3d(), definition.decor.depth/2D, definition.decor.height/2D, definition.decor.width/2D);
		this.boundingBoxes[2] = boundingBoxes[0];
		this.boundingBoxes[3] = boundingBoxes[1];
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			//Reset clicked state.
			if(variablesOn.contains("clicked")){
				variablesOn.remove("clicked");
			}
			
			//TODO remove legacy rotation code in V21.
			if(!converted){
				if(!world.isAir(position)){
					this.orientation.y = -world.getBlockRotation(position);
					this.prevOrientation.y = orientation.y;
					converted = true;
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public RenderDecor getRenderer(){
		if(renderer == null){
			renderer = new RenderDecor();
		}
		return renderer;
	}
}
