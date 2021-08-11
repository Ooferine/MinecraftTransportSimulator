package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityPole;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

/**Pole block class.  This class allows for dynamic collision boxes and dynamic
 * placement of components on poles via the Tile Entity.
 *
 * @author don_bruce
 */
public class BlockPole extends ABlockBaseTileEntity{
	
	public BlockPole(){
		super(10.0F, 5.0F);
	}
	
	@Override
	public TileEntityPole createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityPole(world, position, data);
	}
}
