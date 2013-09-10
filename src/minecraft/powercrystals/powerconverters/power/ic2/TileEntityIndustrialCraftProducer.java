package powercrystals.powerconverters.power.ic2;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergySource;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.MinecraftForge;
import powercrystals.powerconverters.PowerConverterCore;
import powercrystals.powerconverters.power.TileEntityEnergyProducer;

public class TileEntityIndustrialCraftProducer extends TileEntityEnergyProducer<IEnergyAcceptor> implements IEnergySource
{
    private double energy;
	private boolean _isAddedToEnergyNet;
	private boolean _didFirstAddToNet;
	
	private int _packetCount;
	
	public TileEntityIndustrialCraftProducer()
	{
		this(0);
	}
	
	public TileEntityIndustrialCraftProducer(int voltageIndex)
	{
		super(PowerConverterCore.powerSystemIndustrialCraft, voltageIndex, IEnergyAcceptor.class);
		if(voltageIndex == 0)
		{
			_packetCount = PowerConverterCore.throttleIC2LVProducer.getInt();
		}
		else if(voltageIndex == 1)
		{
			_packetCount = PowerConverterCore.throttleIC2MVProducer.getInt();
		}
		else if(voltageIndex == 2)
		{
			_packetCount = PowerConverterCore.throttleIC2HVProducer.getInt();
		}
		else if(voltageIndex == 3)
		{
			_packetCount = PowerConverterCore.throttleIC2EVProducer.getInt();
		}
	}
	
	@Override
	public void updateEntity()
	{
		super.updateEntity();
		if(!_didFirstAddToNet && !worldObj.isRemote)
		{
			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
			_didFirstAddToNet = true;
			_isAddedToEnergyNet = true;
		}
	}
	
	@Override
	public void validate()
	{
		super.validate();
		if(!_isAddedToEnergyNet)
		{
			_didFirstAddToNet = false;
		}
	}
	
	@Override
	public void invalidate()
	{
		if(_isAddedToEnergyNet)
		{
			if(!worldObj.isRemote)
			{
				MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
			}
			_isAddedToEnergyNet = false;
		}
		super.invalidate();
	}
	
	@Override
	public double produceEnergy(double energy)
	{
		if(!_isAddedToEnergyNet)
		{
			return energy;
		}
		
		double eu = energy / PowerConverterCore.powerSystemIndustrialCraft.getInternalEnergyPerOutput();
		
		for(int i = 0; i < _packetCount; i++)
		{
			double producedEu = Math.min(eu, getMaxEnergyOutput());
            energy += producedEu;
            eu -= producedEu;
            if (eu < getMaxEnergyOutput())
                break;
            /*
			EnergyTileSourceEvent e = new EnergyTileSourceEvent(this, producedEu);
			MinecraftForge.EVENT_BUS.post(e);
			eu -= (producedEu - e.amount);
			if(e.amount == producedEu || eu < getMaxEnergyOutput())
			{
				break;
			}
			*/
		}
		this.energy = eu * PowerConverterCore.powerSystemIndustrialCraft.getInternalEnergyPerOutput();
		return this.energy;
	}

	@Override
	public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction)
	{
		return true;
	}

	//@Override
	public int getMaxEnergyOutput()
	{
		return getPowerSystem().getVoltageValues()[getVoltageIndex()];
	}

    /**
     * Energy output provided by the source this tick.
     * This is typically Math.min(stored energy, max output/tick).
     *
     * @return Energy offered this tick
     */
    @Override
    public double getOfferedEnergy() {
        return energy;
    }

    /**
     * Draw energy from this source's buffer.
     * <p/>
     * If the source doesn't have a buffer, this is a no-op.
     *
     * @param amount amount of EU to draw, may be negative
     */
    @Override
    public void drawEnergy(double amount) {
        energy -= amount;
    }
}