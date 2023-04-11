/*
 * Copyright 2013-2016 OCSInventory-NG/AndroidAgent contributors : mortheres, cdpointpoint,
 * Cédric Cabessa, Nicolas Ricquemaque, Anael Mobilia
 *
 * This file is part of OCSInventory-NG/AndroidAgent.
 *
 * OCSInventory-NG/AndroidAgent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * OCSInventory-NG/AndroidAgent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OCSInventory-NG/AndroidAgent. if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package oaf.ocs.android.sections;

import android.util.Log;

import oaf.ocs.android.actions.OCSLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCSDrives implements OCSSectionInterface {
    final private String sectionTag = "DRIVES";

    private final String DFPATH = "/system/bin/df";
    private final String MOUNTSPATH = "/proc/mounts";

    public ArrayList<OCSDrive> drives;

    public OCSDrives() {
        drives = new ArrayList<>();
        OCSLog ocslog = OCSLog.getInstance();
        // Lecture des FS a partir de la commande df
        try {
            // Lancement de la commande
            InputStream is = new ProcessBuilder(DFPATH).start().getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is), 8192);
            String ligne;
            while ((ligne = br.readLine()) != null) {
                Log.i("df : ", ligne);
                // Parsing the df line
                String strPattern = "^(/.*?):*\\s.*";
                Pattern p = Pattern.compile(strPattern, Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(ligne);
                if (m.find() && m.group(1) != null) {
                    ocslog.debug("Add drive " + m.group(1));
                    try {
                        OCSDrive drive = new OCSDrive(m.group(1).trim());
                        drives.add(drive);
                    } catch (IllegalArgumentException e) {
                        ocslog.debug("Error - adding drive " + m.group(1) + e.toString());
                    }
                }
            }
            is.close();
        } catch (IOException localIOException) {
            Log.e("ERREUR", "Message :" + localIOException.getMessage());
        }
        /*
        * Complement avec le fichier /proc/mounts
		*/
        try {
            File f = new File(MOUNTSPATH);
            BufferedReader bReader = new BufferedReader(new FileReader(f), 8192);
            String line;
            while ((line = bReader.readLine()) != null) {
                ocslog.debug(line);
                Pattern p = Pattern.compile("(.*?)\\s+(.*?)\\s+(.*?)\\s.*", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(line);
                if (m.find()) {
                    String dev = m.group(1);
                    String type = m.group(2);
                    String fs = m.group(3);
                    ocslog.debug("Volumename :" + dev);
                    ocslog.debug("type       :" + type);
                    ocslog.debug("filesystem :" + fs);

                    for (int i = 0; i < drives.size(); i++) {
                        OCSDrive d = drives.get(i);
                        if (type.matches(d.getType())) {
                            ocslog.debug("MATCH       :" + type);
                            d.setFilesystem(fs);
                            d.setVolumName(dev);
                            break;
                        }
                    }
                }
            }
            bReader.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public String toXML() {
        StringBuilder strOut = new StringBuilder();
        for (OCSDrive o : drives) {
            strOut.append(o.toXml());
        }
        return strOut.toString();
    }

    public String toString() {
        StringBuilder strOut = new StringBuilder();
        for (OCSDrive o : drives) {
            strOut.append(o.toString());
        }
        return strOut.toString();
    }

    public ArrayList<OCSSection> getSections() {
        ArrayList<OCSSection> lst = new ArrayList<>();
        for (OCSDrive o : drives) {
            lst.add(o.getSection());
        }
        return lst;
    }

    public String getSectionTag() {
        return sectionTag;
    }
}