package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.instances.BlockPole;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityPole_Component;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.items.instances.ItemItem.ItemComponentType;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.items.instances.ItemPoleComponent.PoleComponentType;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest.EntityGUIType;
import minecrafttransportsimulator.packets.instances.PacketTileEntityPoleChange;
import minecrafttransportsimulator.rendering.instances.RenderPole;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.minecraft.item.ItemStack;

/**Pole tile entity.  Remembers what components we have attached and the state of the components.
 * This tile entity does not tick, as states can be determined without ticks or are controlled
 * from other tickable TEs.
*
* @author don_bruce
*/
public class TileEntityPole extends ATileEntityBase<JSONPoleComponent>{
	public final Map<Axis, ATileEntityPole_Component> components = new HashMap<Axis, ATileEntityPole_Component>();
	
	private static RenderPole renderer;
	
	private float maxTotalLightLevel;
	
	public TileEntityPole(WrapperWorld world, Point3d position, WrapperNBT data){
		super(world, position, data);
		//Load components back in.
		for(Axis axis : Axis.values()){
			WrapperNBT componentData = data.getData(axis.name());
			if(componentData != null){
				ATileEntityPole_Component newComponent = PoleComponentType.createComponent(this, axis, componentData);
				components.put(axis, newComponent);
			}else if(axis.equals(Axis.NONE)){
				//Add our core component to the NONE axis.
				//This is done for ease of rendering and lookup routines.
				components.put(axis, PoleComponentType.createComponent(this, axis, getItem().validateData(null)));
			}
		}
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			//Update positions for our components.
			//Make sure to get the min/max bounds of the components for bounding box calcs.
			for(Axis axis : Axis.values()){
				if(components.containsKey(axis)){
					ATileEntityPole_Component component = components.get(axis);
					component.update();
					if(axis.equals(Axis.NONE)){
						component.position.setTo(position);
						component.angles.setTo(angles);
					}else{
						component.position.set(0, 0, definition.pole.radius + 0.001).rotateY(axis.yRotation).add(position);
						component.angles.set(0, axis.yRotation, 0).add(angles);
					}
					component.prevPosition.setTo(component.position);
					component.prevAngles.setTo(component.angles);
				}
				
				boundingBox.widthRadius = definition.pole.radius;
				boundingBox.heightRadius = definition.pole.radius;
				boundingBox.depthRadius = definition.pole.radius;
				boundingBox.localCenter.set(0, 0, 0);
				if(axis.blockBased){
					if(world.getBlock(axis.getOffsetPoint(position)) instanceof BlockPole || world.isBlockSolid(axis.getOffsetPoint(position), axis.getOpposite()) || components.containsKey(axis)){
						switch(axis){
							case NORTH: {
								if(boundingBox.depthRadius == definition.pole.radius){
									boundingBox.depthRadius = definition.pole.radius + 0.25;
									boundingBox.localCenter.z = position.z - (0.5 - definition.pole.radius)/2D;
								}else{
									boundingBox.depthRadius = 0.5;
									boundingBox.globalCenter.z = position.z;
								}
								break;
							}
							case SOUTH: {
								if(boundingBox.depthRadius == definition.pole.radius){
									boundingBox.depthRadius = (definition.pole.radius + 0.5)/2D;
									boundingBox.globalCenter.z = position.z + (0.5 - definition.pole.radius)/2D;
								}else{
									boundingBox.depthRadius = 0.5;
									boundingBox.globalCenter.z = position.z;
								}
								break;
							}
							case EAST: {
								if(boundingBox.widthRadius == definition.pole.radius){
									boundingBox.widthRadius = (definition.pole.radius + 0.5)/2D;
									boundingBox.globalCenter.x = position.x - (0.5 - definition.pole.radius)/2D;
								}else{
									boundingBox.widthRadius = 0.5;
									boundingBox.globalCenter.x = position.x;
								}
								break;
							}
							case WEST: {
								if(boundingBox.widthRadius == definition.pole.radius){
									boundingBox.widthRadius = (definition.pole.radius + 0.5)/2D;
									boundingBox.globalCenter.x = position.x + (0.5 - definition.pole.radius)/2D;
								}else{
									boundingBox.widthRadius = 0.5;
									boundingBox.globalCenter.x = position.x;
								}
								break;
							}
							case UP: {
								if(boundingBox.heightRadius == definition.pole.radius){
									boundingBox.heightRadius = (definition.pole.radius + 0.5)/2D;
									boundingBox.globalCenter.y = position.y + (0.5 - definition.pole.radius)/2D;
								}else{
									boundingBox.depthRadius = 0.5;
									boundingBox.globalCenter.y = position.y;
								}
								break;
							}
							case DOWN: {
								if(boundingBox.heightRadius == definition.pole.radius){
									boundingBox.heightRadius = (definition.pole.radius + 0.5)/2D;
									boundingBox.globalCenter.y = position.y - (0.5 - definition.pole.radius)/2D;
								}else{
									boundingBox.depthRadius = 0.5;
									boundingBox.globalCenter.y = position.y;
								}
								break;
							}
							default: break;
						}
					}
				}
			}
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public boolean interact(WrapperPlayer player){
		//Fire a packet to interact with this pole.  Will either add, remove, or allow editing of the pole.
		//Only fire packet if player is holding a pole component that's not an actual pole, a wrench,
		//or is clicking a sign with text.
		TileEntityPole pole = (TileEntityPole) world.getTileEntity(position);
		if(pole != null){
			Axis axis = Axis.getFromRotation(player.getYaw(), pole.definition.pole.allowsDiagonals).getOpposite();
			ItemStack heldStack = player.getHeldStack();
			AItemBase heldItem = player.getHeldItem();
			ATileEntityPole_Component clickedComponent = pole.components.get(axis);
			if(!ConfigSystem.configObject.general.opSignEditingOnly.value || player.isOP()){
				if(heldItem instanceof ItemItem && ((ItemItem) heldItem).definition.item.type.equals(ItemComponentType.WRENCH)){
					//Holding a wrench, try to remove the component.
					//Need to check if it will fit in the player's inventory.
					if(pole.components.containsKey(axis)){
						ATileEntityPole_Component component = pole.components.get(axis);
						WrapperNBT removedComponentData = component.save(new WrapperNBT());
						if(player.isCreative() || player.getInventory().addItem(component.getItem(), removedComponentData)){
							pole.components.remove(axis).remove();
							pole.updateLightState();
							InterfacePacket.sendToAllClients(new PacketTileEntityPoleChange(this, axis, null));
						}
						return true;
					}
				}else if(clickedComponent instanceof TileEntityPole_Sign && clickedComponent.definition.rendering != null && clickedComponent.definition.rendering.textObjects != null){
					//Player clicked a sign with text.  Open the GUI to edit it.
					player.sendPacket(new PacketEntityGUIRequest(clickedComponent, player, EntityGUIType.TEXT_EDITOR));
					return true;
				}else if(heldItem instanceof ItemPoleComponent && !((ItemPoleComponent) heldItem).definition.pole.type.equals(PoleComponentType.CORE)){
					//Player is holding component that could be added.  Try and do so.
					ItemPoleComponent componentItem = (ItemPoleComponent) heldItem;
					ATileEntityPole_Component newComponent = PoleComponentType.createComponent(pole, axis, componentItem.validateData(new WrapperNBT(heldStack)));
					pole.components.put(axis, newComponent);
					pole.updateLightState();
					if(!player.isCreative()){
						player.getInventory().removeStack(player.getHeldStack(), 1);
					}
					InterfacePacket.sendToAllClients(new PacketTileEntityPoleChange(this, axis, newComponent.save(new WrapperNBT())));
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public void remove(){
		super.remove();
		//Remove components as these all come with the main pole.
		for(ATileEntityPole_Component component : components.values()){
			component.remove();
		}
	}
	
	@Override
    public int getRotationIncrement(){
		return 360;
    }
	
	/**
	 * Helper method to update light state and re-do world lighting if required.
	 */
	public void updateLightState(){
		float calculatedLevel = 0;
		for(ATileEntityPole_Component component : components.values()){
			calculatedLevel = Math.max(calculatedLevel, component.getLightProvided());
		}
		if(maxTotalLightLevel != calculatedLevel){
			world.updateLightBrightness(position);
		}
	}
	
	@Override
	public float getLightProvided(){
		return maxTotalLightLevel;
	}
	
	@Override
	public void addDropsToList(List<ItemStack> drops){
		for(Axis axis : Axis.values()){
			if(components.containsKey(axis)){
				drops.add(components.get(axis).getItem().getNewStack());
			}
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public RenderPole getRenderer(){
		if(renderer == null){
			renderer = new RenderPole();
		}
		return renderer;
	}
	
	@Override
    public WrapperNBT save(WrapperNBT data){
		super.save(data);
		//Save all components.
		for(Entry<Axis, ATileEntityPole_Component> connectedObjectEntry : components.entrySet()){
			data.setData(connectedObjectEntry.getKey().name(), connectedObjectEntry.getValue().save(new WrapperNBT()));
		}
		return data;
	}
}
