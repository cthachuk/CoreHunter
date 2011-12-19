// Copyright 2008 Chris Thachuk (chris.thachuk@gmail.com)
//
// This file is part of Core Hunter.

// Core Hunter is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// Core Hunter is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with Core Hunter.  If not, see <http://www.gnu.org/licenses/>.

package org.cimmyt.corehunter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Dataset for DArTValues
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 */
public final class DArTDataset extends AccessionDataset<DArTValue> {
	
	public DArTDataset(Collection<String> accessions, Collection<String> markers) {
		super(accessions, markers);
	}
}

