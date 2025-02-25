package minecrafttransportsimulator.guis.instances;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.TrailerConnection;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.entities.instances.PartEngine;
import minecrafttransportsimulator.guis.components.GUIComponentLabel;
import minecrafttransportsimulator.guis.components.GUIComponentSelector;
import minecrafttransportsimulator.guis.components.GUIComponentTextBox;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketEntityTrailerConnection;
import minecrafttransportsimulator.packets.instances.PacketEntityVariableToggle;
import minecrafttransportsimulator.packets.instances.PacketPartEngine;
import minecrafttransportsimulator.packets.instances.PacketPartEngine.Signal;
import minecrafttransportsimulator.packets.instances.PacketVehicleBeaconChange;
import minecrafttransportsimulator.packets.instances.PacketVehicleControlDigital;


/**A GUI/control system hybrid, this takes the place of the HUD when called up.
 * Used for controlling engines, lights, trim, and other things.
 * 
 * @author don_bruce
 */
public class GUIPanelGround extends AGUIPanel{
	private static final int LIGHT_TEXTURE_WIDTH_OFFSET = 0;
	private static final int LIGHT_TEXTURE_HEIGHT_OFFSET = 196;
	private static final int TURNSIGNAL_TEXTURE_WIDTH_OFFSET = LIGHT_TEXTURE_WIDTH_OFFSET + 20;
	private static final int TURNSIGNAL_TEXTURE_HEIGHT_OFFSET = 176;
	//private static final int EMERGENCY_TEXTURE_WIDTH_OFFSET = TURNSIGNAL_TEXTURE_WIDTH_OFFSET + 20;
	//private static final int EMERGENCY_TEXTURE_HEIGHT_OFFSET = 216;
	//private static final int SIREN_TEXTURE_WIDTH_OFFSET = EMERGENCY_TEXTURE_WIDTH_OFFSET + 20;
	//private static final int SIREN_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int ENGINE_TEXTURE_WIDTH_OFFSET = TURNSIGNAL_TEXTURE_WIDTH_OFFSET + 20 + 20 + 20;
	private static final int ENGINE_TEXTURE_HEIGHT_OFFSET = 196;
	private static final int TRAILER_TEXTURE_WIDTH_OFFSET = ENGINE_TEXTURE_WIDTH_OFFSET + 20;
	private static final int TRAILER_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int REVERSE_TEXTURE_WIDTH_OFFSET = TRAILER_TEXTURE_WIDTH_OFFSET + 20;
	private static final int REVERSE_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int CRUISECONTROL_TEXTURE_WIDTH_OFFSET = REVERSE_TEXTURE_WIDTH_OFFSET + 20;
	private static final int CRUISECONTROL_TEXTURE_HEIGHT_OFFSET = 216;
	private static final int GEAR_TEXTURE_WIDTH_OFFSET = CRUISECONTROL_TEXTURE_WIDTH_OFFSET + 20;
	private static final int GEAR_TEXTURE_HEIGHT_OFFSET = 176;
	private static final int CUSTOM_TEXTURE_WIDTH_OFFSET = GEAR_TEXTURE_WIDTH_OFFSET + 20;
	private static final int CUSTOM_TEXTURE_HEIGHT_OFFSET = 216;
	
	private GUIComponentSelector lightSelector;
	private GUIComponentSelector turnSignalSelector;
	private GUIComponentSelector reverseSelector;
	private GUIComponentSelector cruiseControlSelector;
	private GUIComponentSelector gearSelector;
	private GUIComponentTextBox beaconBox;
	private final Map<Byte, GUIComponentSelector> engineSelectors = new HashMap<Byte, GUIComponentSelector>();
	private final List<GUIComponentSelector> trailerSelectors = new ArrayList<GUIComponentSelector>();
	private final List<GUIComponentSelector> customSelectors = new ArrayList<GUIComponentSelector>();
	
	public GUIPanelGround(EntityVehicleF_Physics groundVehicle){
		super(groundVehicle);
	}
	
	@Override
	protected void setupLightComponents(int guiLeft, int guiTop){
		//Create a tri-state selector for the running lights and headlights.
		//For the tri-state we need to make sure we don't try to turn on running lights if we don't have any.
		if(vehicle.definition.motorized.hasRunningLights || vehicle.definition.motorized.hasHeadlights){
			lightSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 0*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, "LIGHTS", vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, LIGHT_TEXTURE_WIDTH_OFFSET, LIGHT_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					if(leftSide){
						if(selectorState == 2){
							InterfacePacket.sendToServer(new PacketEntityVariableToggle(vehicle, "headlight"));
						}else if(selectorState == 1){
							InterfacePacket.sendToServer(new PacketEntityVariableToggle(vehicle, "running_light"));
						}
					}else{
						if(selectorState == 0){
							if(vehicle.definition.motorized.hasRunningLights){
								InterfacePacket.sendToServer(new PacketEntityVariableToggle(vehicle, "running_light"));
							}else{
								InterfacePacket.sendToServer(new PacketEntityVariableToggle(vehicle, "headlight"));
							}
						}else if(selectorState == 1){
							InterfacePacket.sendToServer(new PacketEntityVariableToggle(vehicle, "headlight"));
						}
					}
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(lightSelector);
		}
		
		//Add the turn signal selector if we have turn signals.
		if(vehicle.definition.motorized.hasTurnSignals){
			turnSignalSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 1*(GAP_BETWEEN_SELECTORS + SELECTOR_SIZE), SELECTOR_SIZE, SELECTOR_SIZE, "TURNSGNL", vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, TURNSIGNAL_TEXTURE_WIDTH_OFFSET, TURNSIGNAL_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					if(leftSide){
						InterfacePacket.sendToServer(new PacketEntityVariableToggle(vehicle, "left_turn_signal"));
					}else{
						InterfacePacket.sendToServer(new PacketEntityVariableToggle(vehicle, "right_turn_signal"));
					}
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(turnSignalSelector);
		}
		
		if(vehicle.definition.motorized.hasRadioNav){
			//Add beacon text box.  This is at the bottom of the light column where the siren used to be.
			beaconBox = new GUIComponentTextBox(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE*2, vehicle.selectedBeaconName, SELECTOR_SIZE, vehicle.selectedBeacon != null ? Color.GREEN : Color.RED, Color.BLACK, 5){
				@Override
				public void handleKeyTyped(char typedChar, int typedCode, TextBoxControlKey control){
					super.handleKeyTyped(typedChar, typedCode, control);
					//Update the vehicle beacon state.
					InterfacePacket.sendToServer(new PacketVehicleBeaconChange(vehicle, getText()));
				}
			};
			addTextBox(beaconBox);
			
			//Add beacon text box label.
			GUIComponentLabel beaconLabel = new GUIComponentLabel(beaconBox.x + beaconBox.width/2, beaconBox.y + beaconBox.height + 1, vehicle.definition.motorized.panelTextColor != null ? Color.decode(vehicle.definition.motorized.panelTextColor) : Color.WHITE, InterfaceCore.translate("gui.panel.beacon"), null, TextPosition.CENTERED, 0, 0.75F, false);
			beaconLabel.setBox(beaconBox);
			labels.add(beaconLabel);
		}
	}
	
	@Override
	protected void setupEngineComponents(int guiLeft, int guiTop){
		engineSelectors.clear();
		//Create the engine selectors for this vehicle.
		for(Byte engineNumber : vehicle.engines.keySet()){
			//Go to next column if we are on our 5th engine.
			if(engineNumber == 5){
				xOffset += SELECTOR_SIZE + GAP_BETWEEN_SELECTORS;
			}
			GUIComponentSelector engineSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + (SELECTOR_SIZE + GAP_BETWEEN_SELECTORS)*(engineNumber%4), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.engine"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, ENGINE_TEXTURE_WIDTH_OFFSET, ENGINE_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					if(selectorState == 0 && !leftSide){
						InterfacePacket.sendToServer(new PacketPartEngine(vehicle.engines.get(engineNumber), Signal.MAGNETO_ON));
					}else if(selectorState == 1 && !leftSide){
						InterfacePacket.sendToServer(new PacketPartEngine(vehicle.engines.get(engineNumber), Signal.ES_ON));
					}else if(selectorState == 1 && leftSide){
						InterfacePacket.sendToServer(new PacketPartEngine(vehicle.engines.get(engineNumber), Signal.MAGNETO_OFF));
					}else if(selectorState == 2 && leftSide){
						if(vehicle.engines.get(engineNumber).definition.engine.disableAutomaticStarter){
							InterfacePacket.sendToServer(new PacketPartEngine(vehicle.engines.get(engineNumber), Signal.MAGNETO_OFF));
						}else{
							InterfacePacket.sendToServer(new PacketPartEngine(vehicle.engines.get(engineNumber), Signal.ES_OFF));
						}
					}
				}
				
				@Override
				public void onReleased(){
					if(selectorState == 2){
						InterfacePacket.sendToServer(new PacketPartEngine(vehicle.engines.get(engineNumber), Signal.ES_OFF));
					}
				}
			};
			engineSelectors.put(engineNumber, engineSelector);
			addSelector(engineSelector);
		}
		
		//If we have both reverse AND cruise control, render them side-by-side. Otherwise just render one in the middle
		if(haveReverseThrustOption && vehicle.definition.motorized.hasAutopilot){
			reverseSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.reverse"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, REVERSE_TEXTURE_WIDTH_OFFSET, REVERSE_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.REVERSE, !vehicle.reverseThrust));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(reverseSelector);
			
			cruiseControlSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.cruisecontrol"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, CRUISECONTROL_TEXTURE_WIDTH_OFFSET, CRUISECONTROL_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.AUTOPILOT, !vehicle.autopilot));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(cruiseControlSelector);
		}else if(haveReverseThrustOption){
			reverseSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE/2, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.reverse"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, REVERSE_TEXTURE_WIDTH_OFFSET, REVERSE_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.REVERSE, !vehicle.reverseThrust));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(reverseSelector);
		}else if(vehicle.definition.motorized.hasAutopilot){
			cruiseControlSelector = new GUIComponentSelector(guiLeft + xOffset + SELECTOR_SIZE/2, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.cruisecontrol"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, CRUISECONTROL_TEXTURE_WIDTH_OFFSET, CRUISECONTROL_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.AUTOPILOT, !vehicle.autopilot));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(cruiseControlSelector);
		}
	}
	
	@Override
	protected void setupGeneralComponents(int guiLeft, int guiTop){
		//Create up to 8 trailer selectors.  Note that not all may be rendered.
		trailerSelectors.clear();
		for(int i=0; i<8; ++i){
			//Go to next column if we are on our 4th trailer selector.
			if(i == 4){
				xOffset += SELECTOR_SIZE + GAP_BETWEEN_SELECTORS;
			}
			
			if(trailerSwitchDefs.size() > i){
				SwitchEntry switchDef = trailerSwitchDefs.get(i);
				GUIComponentSelector trailerSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + (i%4)*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, switchDef.connectionGroup.groupName, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, TRAILER_TEXTURE_WIDTH_OFFSET, TRAILER_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
					@Override
					public void onClicked(boolean leftSide){
						InterfacePacket.sendToServer(new PacketEntityTrailerConnection(switchDef.entityOn, InterfaceClient.getClientPlayer(), switchDef.connectionGroupIndex));
					}
					
					@Override
					public void onReleased(){}
				};
				trailerSelectors.add(trailerSelector);
				addSelector(trailerSelector);
			}
		}
		
		//If we have gear, add a selector for it.
		//This is rendered on the 4th row.  It is assumed that this will never be combined with 8 trailers...
		if(vehicle.definition.motorized.gearSequenceDuration != 0){
			gearSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + 3*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, InterfaceCore.translate("gui.panel.gear"), vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, GEAR_TEXTURE_WIDTH_OFFSET, GEAR_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketVehicleControlDigital(vehicle, PacketVehicleControlDigital.Controls.GEAR, !vehicle.gearUpCommand));
				}
				
				@Override
				public void onReleased(){}
			};
			addSelector(gearSelector);
		}
	}
	
	@Override
	public void setupCustomComponents(int guiLeft, int guiTop){
		//Add custom selectors if we have any.
		//These are the right-most selector and are vehicle-specific.
		Set<String> customVariables = new LinkedHashSet<String>();
		if(vehicle.definition.rendering.customVariables != null){
			customVariables.addAll(vehicle.definition.rendering.customVariables);
		}
		for(APart part : vehicle.parts){
			if(part.definition.rendering != null && part.definition.rendering.customVariables != null){
				customVariables.addAll(part.definition.rendering.customVariables);
			}
		}
		int variableNumber = 0;
		customSelectors.clear();
		for(String customVariable : customVariables){
			GUIComponentSelector customSelector = new GUIComponentSelector(guiLeft + xOffset, guiTop + GAP_BETWEEN_SELECTORS + (variableNumber%4)*(SELECTOR_SIZE + GAP_BETWEEN_SELECTORS), SELECTOR_SIZE, SELECTOR_SIZE, customVariable, vehicle.definition.motorized.panelTextColor, vehicle.definition.motorized.panelLitTextColor, SELECTOR_TEXTURE_SIZE, SELECTOR_TEXTURE_SIZE, CUSTOM_TEXTURE_WIDTH_OFFSET, CUSTOM_TEXTURE_HEIGHT_OFFSET, getTextureWidth(), getTextureHeight()){
				@Override
				public void onClicked(boolean leftSide){
					InterfacePacket.sendToServer(new PacketEntityVariableToggle(vehicle, this.text));
				}
				
				@Override
				public void onReleased(){}
			};
			customSelectors.add(customSelector);
			addSelector(customSelector);
			++variableNumber;
		}
	}
	
	@Override
	public void setStates(){
		//Set the state of the light selector.
		if(lightSelector != null){
			lightSelector.selectorState = vehicle.variablesOn.contains("headlight") ? 2 : (vehicle.variablesOn.contains("running_light") ? 1 : 0);
		}
		
		//Set the state of the turn signal selector.
		if(turnSignalSelector != null){
			boolean halfSecondClock = inClockPeriod(20, 10);
			if(vehicle.variablesOn.contains("left_turn_signal") && halfSecondClock){
				if(vehicle.variablesOn.contains("right_turn_signal")){
					turnSignalSelector.selectorState = 3;
				}else{
					turnSignalSelector.selectorState = 1;
				}
			}else if(vehicle.variablesOn.contains("right_turn_signal") && halfSecondClock){
				turnSignalSelector.selectorState = 2;
			}else{
				turnSignalSelector.selectorState = 0;
			}
		}
		
		//Set the state of the engine selectors.
		for(Entry<Byte, GUIComponentSelector> engineEntry : engineSelectors.entrySet()){
			if(vehicle.engines.containsKey(engineEntry.getKey())){
				PartEngine engine = vehicle.engines.get(engineEntry.getKey());
				if(engine.definition.engine.disableAutomaticStarter){
					engineEntry.getValue().selectorState = engine.state.magnetoOn ? 2 : 0;
				}else{
					engineEntry.getValue().selectorState = engine.state.magnetoOn ? (engine.state.esOn ? 2 : 1) : 0;
				}
			}
		}
		
		//If we have reverse thrust, set the selector state.
		if(reverseSelector != null){
			reverseSelector.selectorState = vehicle.reverseThrust ? 1 : 0;
		}
		
		//If we have cruise control, set the selector state.
		if(cruiseControlSelector != null){
			cruiseControlSelector.selectorState = vehicle.autopilot ? 1 : 0;
		}
		
		//If we have gear, set the selector state.
		if(gearSelector != null){
			if(vehicle.gearUpCommand){
				gearSelector.selectorState = vehicle.gearMovementTime == vehicle.definition.motorized.gearSequenceDuration ? 2 : 3;
			}else{
				gearSelector.selectorState = vehicle.gearMovementTime == 0 ? 0 : 1;
			}
		}
		
		//Iterate through trailers and set the visibility of the trailer selectors based on their state.
		for(int i=0; i<trailerSelectors.size(); ++i){
			GUIComponentSelector trailerSelector = trailerSelectors.get(i);
			SwitchEntry switchDef = trailerSwitchDefs.get(i);
			trailerSelector.visible = true;
			if(switchDef.connectionGroup.hookup){
				trailerSelector.selectorState = switchDef.entityOn.towedByConnection != null ? 0 : 1;
			}else{
				trailerSelector.selectorState = 1;
				for(TrailerConnection connection : switchDef.entityOn.getTowingConnections()){
					if(connection.hitchGroupIndex == switchDef.connectionGroupIndex){
						trailerSelector.selectorState = 0;
						break;
					}
				}
			}
		}
		
		//Set the beaconBox text color depending on if we have an active beacon.
		if(beaconBox != null){
			beaconBox.fontColor = vehicle.selectedBeacon != null ? Color.GREEN : Color.RED;
		}
		
		//Iterate through custom selectors and set their states.
		for(GUIComponentSelector customSelector : customSelectors){
			customSelector.selectorState = vehicle.variablesOn.contains(customSelector.text) ? 1 : 0;
		}
	}
}
