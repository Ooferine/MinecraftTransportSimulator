package minecrafttransportsimulator.blocks.tileentities.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.NavBeacon;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.instances.PacketEntityGUIRequest;
import minecrafttransportsimulator.systems.NavBeaconSystem;

/**Beacon tile entity.  Contains code for handling interfacing with
 * the global world saved data and information of the beacon states.
 * Note that the variables for this beacon are saved as textLines
 * in the main decor class to ensure they both display properly,
 * and to allow a common interface for all GUI operations.
 *
 * @author don_bruce
 */
public class TileEntityBeacon extends TileEntityDecor{
	private final JSONText nameTextObject;
	private final JSONText glideslopeTextObject;
	private final JSONText bearingTextObject;
	
	public String beaconName;
	
	public TileEntityBeacon(WrapperWorld world, Point3d position, WrapperNBT data){
		super(world, position, data);
		//Manually add textLines, as these won't be in the JSON.
		this.nameTextObject = new JSONText();
		nameTextObject.attachedTo = "NULL";
		nameTextObject.pos = new Point3d();
		nameTextObject.rot = new Point3d();
		nameTextObject.fieldName = "Beacon Name";
		nameTextObject.maxLength = 5;
		
		this.glideslopeTextObject = new JSONText();
		glideslopeTextObject.attachedTo = "NULL";
		glideslopeTextObject.pos = new Point3d();
		glideslopeTextObject.rot = new Point3d();
		glideslopeTextObject.fieldName = "Glide Slope (Deg)";
		glideslopeTextObject.maxLength = 5;
		
		this.bearingTextObject = new JSONText();
		bearingTextObject.attachedTo = "NULL";
		bearingTextObject.pos = new Point3d();
		bearingTextObject.rot = new Point3d();
		bearingTextObject.fieldName = "Bearing (Deg)";
		bearingTextObject.maxLength = 5;
		
		this.beaconName = data.getString("beaconName");
		if(beaconName.isEmpty()){
			setBeaconToDefault();
		}else{
			NavBeacon beacon = NavBeaconSystem.getBeacon(world, beaconName);
			if(beacon != null){
				text.put(nameTextObject, beacon.name);
				text.put(glideslopeTextObject, String.valueOf(beacon.glideSlope));
				text.put(bearingTextObject, String.valueOf(beacon.bearing));
			}else{
				setBeaconToDefault();
			}
		}
	}
	
    @Override
    public void destroy(BoundingBox box){
    	super.destroy(box);
    	NavBeaconSystem.removeBeacon(world, beaconName);
    }
	
    @Override
	public boolean interact(WrapperPlayer player){
		player.sendPacket(new PacketEntityGUIRequest(this, player, PacketEntityGUIRequest.EntityGUIType.TEXT_EDITOR));
		return true;
	}
	
	@Override
	public void updateText(List<String> textLines){
		NavBeaconSystem.removeBeacon(world, beaconName);
		try{
			beaconName = textLines.get(0);
			text.put(nameTextObject, beaconName);
			text.put(glideslopeTextObject, textLines.get(1));
			text.put(bearingTextObject, textLines.get(2));
			NavBeacon beacon = new NavBeacon(text.get(nameTextObject), Double.valueOf(text.get(glideslopeTextObject)), Double.valueOf(text.get(bearingTextObject)), position);
			NavBeaconSystem.addBeacon(world, beacon);
		}catch(Exception e){
			//Don't save this beacon.  It's entered invalid.
			setBeaconToDefault();
		}
	}
	
	private void setBeaconToDefault(){
		beaconName = "NONE";
		text.put(nameTextObject, beaconName);
		text.put(glideslopeTextObject, "10.0");
		text.put(bearingTextObject, "0.0");
	}
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setString("beaconName", beaconName);
		return data;
	}
}
