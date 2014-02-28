/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.exoplatform.calendar.ws.bean;

import org.exoplatform.webservice.cs.bean.End;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;

public class RepeatResource implements Serializable {
  private static final long serialVersionUID = 5586869269014670514L;

  boolean                   enabled;

  String                    type;

  int                       every;

  String                    repeatOn;

  String                    repeateBy;

  String[]                  exclude;

  End                       end;

  public RepeatResource(boolean enabled,
                        String type,
                        int every,
                        String repeatOn,
                        String repeatBy,
                        Collection<String> exclude,
                        End end) {
    this.enabled = enabled;
    this.type = type;
    this.every = every;
    this.repeatOn = repeatOn;
    this.repeateBy = repeatBy;
    if (exclude != null)
      this.exclude = exclude.toArray(new String[exclude.size()]);
    this.end = end;
  }

}
