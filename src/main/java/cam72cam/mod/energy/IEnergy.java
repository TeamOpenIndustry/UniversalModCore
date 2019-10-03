package cam72cam.mod.energy;

import alexiil.mc.lib.attributes.Simulation;
import io.github.cottonmc.energy.api.DefaultEnergyTypes;
import io.github.cottonmc.energy.api.EnergyAttribute;

public interface IEnergy {
    static IEnergy from(EnergyAttribute internal) {
        return new IEnergy() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return internal.insertEnergy(DefaultEnergyTypes.MEDIUM_VOLTAGE, maxReceive, simulate ? Simulation.SIMULATE : Simulation.ACTION);
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return internal.extractEnergy(DefaultEnergyTypes.MEDIUM_VOLTAGE, maxExtract, simulate ? Simulation.SIMULATE : Simulation.ACTION);
            }

            @Override
            public int getEnergyStored() {
                return internal.getCurrentEnergy();
            }

            @Override
            public int getMaxEnergyStored() {
                return internal.getMaxEnergy();
            }
        };
    }

    //TODO rename fns
    int receiveEnergy(int maxReceive, boolean simulate);

    int extractEnergy(int maxExtract, boolean simulate);

    int getEnergyStored();

    int getMaxEnergyStored();
}
