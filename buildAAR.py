#
# Build an Android .aar file suitable for including in projects in Android Studio
# (c) 2014 Paul LeBeau paul@caverock.com
# Released under the Apache v2 license
#

import sys, os, zipfile, StringIO, ConfigParser


# Configration
# The default locations assume that this script has been placed in a subfolder
# of the project (for example: "<MyProject>/aar").

libraryProjectDir = ".\\"



def main():

  if not os.path.isdir(libraryProjectDir):
    fatal_error("Bad project directory (projectDir) setting")

  # We need the canonical name
  projectDir = os.path.abspath(libraryProjectDir)

  # Where to put the resulting .aar file
  outputDir = os.path.join(projectDir, "bin")

  # Location of the "bin" dir
  binDir = os.path.join(projectDir, "bin")

  # Location of AndroidManifest.xml file
  manifestFile = os.path.join(projectDir, "AndroidManifest.xml")

  # Location of the "res" dir
  resDir = os.path.join(projectDir, "res")

  # Project name
  projectName = os.path.split(projectDir)[1].lower()

  # Read properties file
  propertiesFile = os.path.join(projectDir, "project.properties")
  if not os.path.exists(propertiesFile):
    fatal_error("Could not find project.properties file: "+propertiesFile)
  projectProperties = read_properties(propertiesFile)
  if getProperty(projectProperties, "android.library") != "true":
    fatal_error("This is not a Android library project.")

  # Check that jar file is present
  jarFile = os.path.join(binDir, projectName +".jar")
  if not os.path.exists(jarFile):
    fatal_error('Could not find "' + projectName + '.jar". Did this project build correctly?')
  
  # Check that manifest file is present (mandatory)
  if not os.path.exists(manifestFile):
    fatal_error("Could not find AndroidManifest.xml file. Check config at the top of this script.")
  
  # Check that R.txt file is present (mandatory)
  RtxtFile = os.path.join(binDir, "R.txt")
  if not os.path.exists(RtxtFile):
    fatal_error("Could not find R.txt file. Did this project build correctly?")

    
  log("Constructing aar file...")
  
  tmpFile = os.path.join(binDir, "tmp.aar")
  
  # Cleanup old tmp files
  #if os.path.exists(tmpFile):
  #  os.remove(tmpFile)
  
  with zipfile.ZipFile(tmpFile, 'a') as newZip:
    # Add manifest file to aar
    newZip.write(manifestFile, "AndroidManifest.xml")
    # Add classes.jar, which is just the jar file that the Eclipse ADT tools built
    newZip.write(jarFile, "classes.jar")
    # Add res dir
    if os.path.exists(resDir):
      log("Copying res dir")
      zipWriteDir(newZip, resDir, "res")
    # Add R.txt
    newZip.write(RtxtFile, "R.txt")
    # Add assets dir
    assetsDir = os.path.join(projectDir, "assets")
    if os.path.exists(assetsDir):
      log("Copying assets dir")
      zipWriteDir(newZip, assetsDir, "assets")


  # New aar (tmp) file successfully created.  Rename it to it final name.
  aarFile = os.path.join(binDir, projectName + ".aar")
  if os.path.exists(aarFile):
    os.remove(aarFile)
  os.rename(tmpFile, aarFile);

  # Done
  print '\nDone!  Generated aar file can be found at: ' + aarFile
  


def read_properties(fn):
  config = StringIO.StringIO()
  config.write("[dummysection]\n")
  config.write(open(fn).read())
  config.seek(0, os.SEEK_SET)
  cp = ConfigParser.ConfigParser()
  cp.readfp(config)
  return cp

def getProperty(cp, key):
  return cp.get("dummysection", key)

def zipWriteDir(myzipfile, dirname, arcdirname):
  for dirpath,dirs,files in os.walk(dirname):
    relpath = os.path.relpath(dirpath, dirname)
    for f in files:
      fn = os.path.join(dirpath, f)
      myzipfile.write(fn, os.path.join(arcdirname, relpath, f))

def error(msg):
    print "ERROR: "+ msg
def warning(msg):
    print "WARNING: "+ msg
def log(msg):
    print msg

def fatal_error(msg):
  error(msg)
  exit()  



if __name__ == "__main__":
  main()
