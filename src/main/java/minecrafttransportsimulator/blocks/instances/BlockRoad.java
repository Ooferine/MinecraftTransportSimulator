package minecrafttransportsimulator.blocks.instances;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBaseTileEntity;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockRoad extends ABlockBaseTileEntity{
	
    public BlockRoad(){
    	super(10.0F, 5.0F);
	}
    
    @Override
	public TileEntityRoad createTileEntity(WrapperWorld world, Point3d position, WrapperNBT data){
		return new TileEntityRoad(world, position, data);
	}
}
