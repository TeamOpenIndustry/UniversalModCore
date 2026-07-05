package cam72cam.mod.energy;

import net.minecraftforge.energy.IEnergyStorage;

public interface IEnergy {
    static IEnergy from(IEnergyStorage internal) {
        return new IEnergy() {
            @Override
            public int receive(int maxReceive, boolean simulate) {
                return internal.receiveEnergy(maxReceive, simulate);
            }

            @Override
            public int extract(int maxExtract, boolean simulate) {
                return internal.extractEnergy(maxExtract, simulate);
            }

            @Override
            public int getCurrent() {
                return internal.getEnergyStored();
            }

            @Override
            public int getMax() {
                return internal.getMaxEnergyStored();
            }
        };
    }

    /**
     * Receive energy from some source
     * @param maxReceive Max amount that can be received
     * @param simulate Whether to actually apply the change or not
     * @return the actual amount accepted
     */
    int receive(int maxReceive, boolean simulate);

    /**
     * Extract energy from this source
     * @param maxExtract Max amount that can be extracted
     * @param simulate Whether to actually apply the change or not
     * @return the actual amount extracted
     */
    int extract(int maxExtract, boolean simulate);

    /**
     * @return Current amount of energy stored
     */
    int getCurrent();

    /**
     * @return Max amount of energy that could be stored
     */
    int getMax();
}
