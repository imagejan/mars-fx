/*******************************************************************************
 * Copyright (C) 2019, Duderstadt Lab
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package de.mpg.biochem.mars.fx.molecule.moleculesTab;

import de.mpg.biochem.mars.fx.event.MoleculeEvent;
import de.mpg.biochem.mars.fx.molecule.AbstractPositionOfInterestTable;
import de.mpg.biochem.mars.fx.event.MoleculeIndicatorChangedEvent;
import de.mpg.biochem.mars.molecule.Molecule;
import javafx.event.Event;

public class MoleculePositionOfInterestTable extends AbstractPositionOfInterestTable implements MoleculeSubPane {

    public MoleculePositionOfInterestTable() {        
        super();
    }
    
    @Override
    public void handle(MoleculeEvent event) {
        event.invokeHandler(this);
        event.consume();
    }

	@Override
	public void fireEvent(Event event) {
		getNode().fireEvent(event);
	}

	@Override
	public void onMoleculeSelectionChangedEvent(Molecule molecule) {
		this.record = molecule;
    	loadData();
	}

	@Override
	protected void fireIndicatorChangedEvent() {
		getNode().fireEvent(new MoleculeIndicatorChangedEvent((Molecule) record));
	}

	@Override
	protected void addEventHandlers() {
		getNode().addEventHandler(MoleculeEvent.MOLECULE_EVENT, this);
	}
}
