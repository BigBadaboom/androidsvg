#
# Generate and deploy the jar and associated files to the Sonatype maven repository.
#

import os, re, tempfile, subprocess   #, sys, datetime, zipfile

# Location of the source file that defines the current version
VERSION_FILE = '../src/com/caverock/androidsvg/SVG.java'

# Version regex
VERSION_RE = '\sVERSION\s*=\s*"([\d\w.-]+)"'

# Source pom file
ORIG_POM_FILE = 'src-pom.xml'

# Regex for finding the place in the pom file to insert the version number
POM_VERSION_RE = '{{VERSION}}'

# The jar file to be deployed
JAR_FILE = '../bin/androidsvg_1.2.jar'

# The aar file to be deployed
AAR_FILE = '../bin/androidsvg_1.2.aar'

# The dummy sources and javadoc jars
SOURCES_JAR_FILE = 'androidsvg-sources.jar'
JAVADOC_JAR_FILE = 'androidsvg-javadoc.jar'


def main():

  # Get the current version number of the library
  libraryVersion = get_current_version()
 
  go = raw_input('\nDo maven deploy for version '+libraryVersion+'? (y/N): ')
  if not go in ['Y','y']:
    exit()

  # Get GPG passphrase
  #passphrase = raw_input('GPG passphrase: ')
  #if passphrase == '':
  #  print "Exiting: need passphrase."
  #  exit()

  # Create a temporary file to hold the generated pom file
  print 'Creating POM file for this version...'
  tempPomFile = tempfile.NamedTemporaryFile(suffix='.pom.xml', delete=False)
  #print tempPomFile.name

  # Write out a new pom file with the version number set to the latest version
  srcPomFile = read(ORIG_POM_FILE)
  srcPomFile = re.sub(POM_VERSION_RE, libraryVersion, srcPomFile);
  tempPomFile.write(srcPomFile)
  tempPomFile.close()


  # Sign and deploy the JAR artifact
  print '\nSigning and deploying JAR artifact...'
  basecmd =  'mvn gpg:sign-and-deploy-file'
  basecmd += ' -DpomFile=' + tempPomFile.name
  basecmd += ' -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/'
  basecmd += ' -DrepositoryId=sonatype-nexus-staging'
  #basecmd += ' -Dpassphrase=' + passphrase

  cmd = basecmd
  cmd += ' -Dfile=' + os.path.realpath(JAR_FILE)

  print cmd
  os.system(cmd)


  # Sign and deploy the AAR artifact
  print '\n\n\nSigning and deploying AAR artifact...'

  cmd = basecmd
  cmd += ' -Dfile=' + os.path.realpath(AAR_FILE)
  cmd += ' -Dtype=aar'
  cmd += ' -Dpackaging=aar'

  print cmd
  os.system(cmd)


  # Sign and deploy the dummy sources
  print '\n\n\nSigning and deploying sources jar...'

  cmd = basecmd
  cmd += ' -Dfile=' + os.path.realpath(SOURCES_JAR_FILE)
  cmd += ' -Dclassifier=sources'

  print cmd
  os.system(cmd)


  # Sign and deploy the dummy javadoc
  print '\n\n\nSigning and deploying javadoc jar...'

  cmd = basecmd
  cmd += ' -Dfile=' + os.path.realpath(JAVADOC_JAR_FILE)
  cmd += ' -Dclassifier=javadoc'

  print cmd
  os.system(cmd)

  # Done
  print '\nDone!'



def read(src):
  file = open(os.path.realpath(src), "rb")
  str = file.read()
  file.close()
  return str


def get_current_version():
  versionFile = read(VERSION_FILE)
  m = re.search(VERSION_RE, versionFile)
  if (m):
    return m.group(1)
  else:
    return ""


def error(msg):
  print "ERROR: "+ msg
  exit()  



if __name__ == "__main__":
  main()
