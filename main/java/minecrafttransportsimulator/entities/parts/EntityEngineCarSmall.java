package minecrafttransportsimulator.entities.parts;

import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.core.EntityMultipartParent;
import minecrafttransportsimulator.entities.main.EntityCar;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public abstract class EntityEngineCarSmall extends EntityEngineCar{

	public EntityEngineCarSmall(World world){
		super(world);
	}

	public EntityEngineCarSmall(World world, EntityCar car, String parentUUID, float offsetX, float offsetY, float offsetZ){
		super(world, car, parentUUID, offsetX, offsetY, offsetZ);
	}
	
	@Override
	protected float getSize(){
		return  0.5F;
	}

	@Override
	protected byte getStarterPower(){
		//TODO add new sound.
		return 50;
	}

	@Override
	protected byte getStarterIncrement(){
		//TODO add new sound.
		return 4;
	}

	@Override
	protected String getCrankingSoundName(){
		//TODO add new sound.
		return "small_engine_cranking";
	}

	@Override
	protected String getStartingSoundName(){
		//TODO add new sound.
		return "small_engine_starting";
	}

	@Override
	protected String getRunningSoundName(){
		//TODO add new sound.
		return "small_engine_running";
	}

	@Override
	protected Item getEngineItem(){
		return MTSRegistry.engineCarSmall;
	}
	
	public abstract boolean isAutomatic();
	
	public abstract byte getNumberGears();
	
	public abstract float getRatioForGear(byte gearNumber);
	
	public static class Automatic extends EntityEngineCarSmall{
		public Automatic(World world){
			super(world);
		}

		public Automatic(World world, EntityMultipartParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
			super(world, (EntityCar) parent, parentUUID, offsetX, offsetY, offsetZ);
		}

		@Override
		public boolean isAutomatic(){
			return true;
		}

		@Override
		public byte getNumberGears(){
			return 4;
		}

		@Override
		public float getRatioForGear(byte gearNumber){
			switch(gearNumber){
				case(-1): return -2.0F;
				case(1): return 3.5F;
				case(2): return 2.75F;
				case(3): return 1.5F;
				case(4): return 0.75F;
				default: return 0.0F;
			}
		}
	}
}