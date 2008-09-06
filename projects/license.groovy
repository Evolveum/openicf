/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */

// determine if the license exists..
def license = new File(attributes.get("license") ?: "unknown")
if (!license.isFile()) {
    throw new FileNotFoundException("License not found: ${license}")
}

// create a list of lines that are the license
def lic = []
license.eachLine() { line -> lic << line }

// start processing all the files
def filesets = elements.get("fileset")
filesets.each() { fs ->
    def scanner = fs.getDirectoryScanner(project)
    def files = scanner.getIncludedFiles()
    def basedir = scanner.getBasedir()
    files.each() { fn ->
        addLicense(lic, new File(basedir, fn))
    }
}

def addLicense(def lic, def f) {
    def tmp = null
    def fn = f.toString()
    def idx = fn.indexOf('.')
    def suffix = fn.substring(idx == -1 ? 0 : idx)
    switch (suffix) {
        case '.java':
            tmp = addJavaLicense(lic, f)
            break;
        case '.html':
            tmp = addHtmlLicense(lic, f)
            break
        case '.cs':
            tmp = addCSharpLicense(lic, f)
            break
        case '.properties':
            tmp = addPropertiesLicense(lic, f)
            break
        case '.proj':
        case '.csproj':
        case '.wixproj':
            tmp = addMSBuildLicense(lic, f)
            break
        default:
            // for all ant based builds
            if (fn.endsWith('build.xml')) {
                tmp = addAntBuildLicense(lic, f)
            }
            break
    }
    // copy and delete
    if (tmp) { 
        copyFile(tmp, f)
        tmp.delete()
    } else {
        println "Unsupported file: ${f}"
    }
}


def addPropertiesLicense(def lic, def f) {
    def END_LICENSE = '# -- END LICENSE'
    def startFound = false
    def tmp = newTempFile()
    // determine if it already has a license
    def fileText = f.getText()
    def hasComments = fileText.contains('#')
    def hasLicense = fileText.contains(END_LICENSE)    
    tmp.withPrintWriter { wrt ->
        f.eachLine { line ->
            if (startFound) {
                wrt.println(line)
            } else if (!hasComments || 
                    (!hasLicense && line.startsWith("#")) || 
                    (hasLicense && line.startsWith(END_LICENSE))) { 
                startFound = true
                // write out the license
                wrt.println('# -- START LICENSE')
                lic.each {
                    wrt.print('# ')
                    wrt.println(it)
                }
                wrt.println(END_LICENSE)
                // write out the package line
                if (!hasLicense) {
                    wrt.println(line)
                }
            }
        }
    }
    return tmp
}

def addHtmlLicense(def lic, def f) {
    return addXmlLicense('<html', lic, f)
}

def addAntBuildLicense(def lic, def f) {
    return addXmlLicense('<project', lic, f)
}

def addMSBuildLicense(def lic, def f) {
    return addXmlLicense('<Project', lic, f)
}

def addXmlLicense(def header, def lic, def f) {
    def startFound = false
    def tmp = newTempFile()
    tmp.withPrintWriter { wrt ->
        f.eachLine { line ->
            if (startFound) {
                wrt.println(line)
            } else if (line.startsWith(header)) { 
                startFound = true
                // write out the license
                wrt.println('<!--')
                lic.each {
                    if (!it.contains('----')) {
                        wrt.print('  ')
                        wrt.println(it)
                    }
                }
                wrt.println('-->')
                // write out the package line
                wrt.println(line)
            }
        }
    }
    return tmp
}

def addCSharpLicense(def lic, def f) {
    def startFound = false
    def tmp = newTempFile()
    tmp.withPrintWriter { wrt ->
        f.eachLine { line ->
            if (startFound) {
                wrt.println(line)
            } else if (line.startsWith("using System") 
                    || line.startsWith("#region")) {
                startFound = true
                // write out the license
                wrt.println('/*')
                lic.each {
                    wrt.print(' * ')
                    wrt.println(it)
                }
                wrt.println(' */')
                // write out the package line
                wrt.println(line)
            }
        }
    }
    return tmp
}

def addJavaLicense(def lic, def f) {
    def startFound = false
    def tmp = newTempFile()
    tmp.withPrintWriter { wrt -> 
        f.eachLine { line ->
            if (startFound) {
                wrt.println(line)
            } else if (line.startsWith("package")) {
                // found the end of the current license
                startFound = true
                // write out the license
                wrt.println('/*')
                lic.each {
                    wrt.print(' * ')
                    wrt.println(it)
                }
                wrt.println(' */')
                // write out the package line
                wrt.println(line)
            }
        }
    }
    return tmp
}

def newTempFile() {
    File.createTempFile("license", ".tmp")
}

def copyFile(def src, def dst) {
    def text = src.getText()
    dst.write(text)
}

