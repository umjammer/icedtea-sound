# Makefile.am -- Input for automake
# Copyright (C) 2014 Red Hat, Inc.
#
# This file is part of IcedTea-Sound.
#
# IcedTea-Sound is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License as published by
# the Free Software Foundation, version 2.
# 
# IcedTea-Sound is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with IcedTea-Sound; see the file COPYING.  If not, write to
# the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
# 02110-1301 USA.

TARGET_NAME=lib$(PACKAGE_NAME).so
LIBRARY_NAME=$(TARGET_NAME).$(PACKAGE_VERSION)

# Source directories
ICEDTEA_SOUND_DIR = $(abs_top_srcdir)
ICEDTEA_SOUND_NATIVE_SRCDIR = $(ICEDTEA_SOUND_DIR)/src/native
ICEDTEA_SOUND_NATIVE_SRCS = $(wildcard $(ICEDTEA_SOUND_NATIVE_SRCDIR)/*.c)
ICEDTEA_SOUND_JAVA_SRCDIR = $(ICEDTEA_SOUND_DIR)/src/java
ICEDTEA_SOUND_JAVA_GENDIR = $(abs_top_builddir)/src/java

# Build directories
RUNTIME = $(SYSTEM_JDK_DIR)/jre/lib/rt.jar
ICEDTEA_SOUND_BUILDDIR = $(abs_top_builddir)/build
ICEDTEA_SOUND_NATIVE_BUILDDIR = $(ICEDTEA_SOUND_BUILDDIR)/native
ICEDTEA_SOUND_NATIVE_OBJECTS = \
	$(subst $(ICEDTEA_SOUND_NATIVE_SRCDIR),$(ICEDTEA_SOUND_NATIVE_BUILDDIR),$(patsubst %.c,%.o,$(ICEDTEA_SOUND_NATIVE_SRCS)))
ICEDTEA_SOUND_CLASS_DIR = $(ICEDTEA_SOUND_BUILDDIR)/classes

# Files
# Must use relative paths so as not to break make distcheck
ICEDTEA_SOUND_JAVA_SRCS = $(top_srcdir)/src/java/org/classpath/icedtea/*/*.java \
	$(top_builddir)/src/java/org/classpath/icedtea/Config.java
ICEDTEA_SOUND_SRCS = $(ICEDTEA_SOUND_JAVA_SRCS) \
	$(top_srcdir)/src/java/META-INF/services/javax.sound.sampled.spi.MixerProvider \
	$(top_srcdir)/src/native/*.h \
	$(top_srcdir)/src/native/*.c \
	$(top_srcdir)/testsounds/README \
	$(top_srcdir)/testsounds/startup.wav \
	$(top_srcdir)/testsounds/logout.wav \
	$(top_srcdir)/testsounds/error.wav \
	$(top_srcdir)/unittests/org/classpath/icedtea/*/*.java
EXTRA_DIST = $(ICEDTEA_SOUND_SRCS)

# Flags
IT_LANGUAGE_SOURCE_VERSION=6
IT_CLASS_TARGET_VERSION=6
IT_JAVAC_SETTINGS=-g -encoding utf-8 $(JAVACFLAGS) $(MEMORY_LIMIT) $(PREFER_SOURCE)
IT_JAVACFLAGS=$(IT_JAVAC_SETTINGS) -source $(IT_LANGUAGE_SOURCE_VERSION) -target $(IT_CLASS_TARGET_VERSION)
IT_CFLAGS=$(CFLAGS) $(ARCHFLAG)
LDFLAGS+=-Xlinker -z -Xlinker defs
if ENABLE_DOCS
ITS_PKGS=org.classpath.icedtea org.classpath.icedtea.pulseaudio
JAVADOC_OPTS=-use -keywords -encoding UTF-8 -splitIndex \
 -bottom '<font size="-1"> <a href="http://icedtea.classpath.org/bugzilla">Submit a bug or feature</a></font>'
if JAVADOC_SUPPORTS_J_OPTIONS
JAVADOC_MEM_OPTS=-J-Xmx1024m -J-Xms128m -J-XX:PermSize=32m -J-XX:MaxPermSize=160m
endif
endif

# Top-Level Targets
# =================

all-local: stamps/icedtea-sound.stamp

clean-local: clean-icedtea-sound clean-docs
	rm -f $(abs_top_builddir)/src.zip
	if [ -e stamps ] ; then rmdir stamps ; fi

.PHONY: clean-icedtea-sound

install-exec-local: $(ICEDTEA_SOUND_NATIVE_BUILDDIR)/$(TARGET_NAME)
	${mkinstalldirs} $(DESTDIR)$(libdir)
	${INSTALL_PROGRAM} $(ICEDTEA_SOUND_NATIVE_BUILDDIR)/$(TARGET_NAME) \
	  $(DESTDIR)$(libdir)/$(LIBRARY_NAME)
	ln -sf $(LIBRARY_NAME) $(DESTDIR)$(libdir)/$(TARGET_NAME)

install-data-local: stamps/icedtea-sound-jar.stamp stamps/docs.stamp
	${mkinstalldirs} $(DESTDIR)$(datadir)/$(PACKAGE_NAME)/
	${INSTALL_DATA} icedtea-sound.jar $(DESTDIR)$(datadir)/$(PACKAGE_NAME)
	(cd $(ICEDTEA_SOUND_JAVA_SRCDIR) && \
	   $(ZIP) -qr $(abs_top_builddir)/src.zip org )
	${INSTALL_DATA} src.zip $(DESTDIR)$(datadir)/$(PACKAGE_NAME)
if ENABLE_DOCS
	${mkinstalldirs} $(DESTDIR)$(htmldir)
	(cd ${abs_top_builddir}/docs; \
	 for files in $$($(FIND) . -type f); \
	 do \
	   ${INSTALL_DATA} -D $${files} $(DESTDIR)$(htmldir)/$${files}; \
	 done)
endif

uninstall-local:
	rm -f $(DESTDIR)$(libdir)/$(TARGET_NAME)
	rm -f $(DESTDIR)$(libdir)/$(LIBRARY_NAME)
	rm -f $(DESTDIR)$(datadir)/$(PACKAGE_NAME)/icedtea-sound.jar
	rm -f $(DESTDIR)$(datadir)/$(PACKAGE_NAME)/src.zip
	rmdir $(DESTDIR)$(datadir)/$(PACKAGE_NAME)
	rm -rf $(DESTDIR)$(htmldir)

stamps/icedtea-sound.stamp: stamps/icedtea-sound-jar.stamp \
  $(ICEDTEA_SOUND_NATIVE_BUILDDIR)/$(TARGET_NAME) stamps/docs.stamp
	mkdir -p stamps
	touch $@

stamps/icedtea-sound-class.stamp:
	mkdir -p $(ICEDTEA_SOUND_CLASS_DIR)
	$(abs_top_builddir)/javac $(IT_JAVACFLAGS) -d $(ICEDTEA_SOUND_CLASS_DIR) \
	 -bootclasspath $(RUNTIME) $(ICEDTEA_SOUND_JAVA_SRCS)
	cp  -r $(ICEDTEA_SOUND_JAVA_SRCDIR)/META-INF $(ICEDTEA_SOUND_CLASS_DIR)
	chmod -R ug+w $(ICEDTEA_SOUND_CLASS_DIR)/META-INF
	mkdir -p stamps
	touch $@

stamps/icedtea-sound-jar.stamp: stamps/icedtea-sound-class.stamp
	$(JAR) cf icedtea-sound.jar -C $(ICEDTEA_SOUND_CLASS_DIR) .;
	mkdir -p stamps
	touch $@

stamps/icedtea-sound-headers.stamp: stamps/icedtea-sound-class.stamp
	mkdir -p $(ICEDTEA_SOUND_NATIVE_BUILDDIR)
	$(JAVAH) -d $(ICEDTEA_SOUND_NATIVE_BUILDDIR) -classpath $(ICEDTEA_SOUND_CLASS_DIR) \
	  -J-Xbootclasspath/p:$(ICEDTEA_SOUND_CLASS_DIR) org.classpath.icedtea.pulseaudio.EventLoop ; \
	$(JAVAH) -d $(ICEDTEA_SOUND_NATIVE_BUILDDIR) -classpath $(ICEDTEA_SOUND_CLASS_DIR) \
	  -J-Xbootclasspath/p:$(ICEDTEA_SOUND_CLASS_DIR) org.classpath.icedtea.pulseaudio.Stream ; \
	$(JAVAH) -d $(ICEDTEA_SOUND_NATIVE_BUILDDIR) -classpath $(ICEDTEA_SOUND_CLASS_DIR) \
	  -J-Xbootclasspath/p:$(ICEDTEA_SOUND_CLASS_DIR) org.classpath.icedtea.pulseaudio.Operation; \
	$(JAVAH) -d $(ICEDTEA_SOUND_NATIVE_BUILDDIR) -classpath $(ICEDTEA_SOUND_CLASS_DIR) \
	  -J-Xbootclasspath/p:$(ICEDTEA_SOUND_CLASS_DIR) org.classpath.icedtea.pulseaudio.PulseAudioSourcePort ; \
	$(JAVAH) -d $(ICEDTEA_SOUND_NATIVE_BUILDDIR) -classpath $(ICEDTEA_SOUND_CLASS_DIR) \
	  -J-Xbootclasspath/p:$(ICEDTEA_SOUND_CLASS_DIR) org.classpath.icedtea.pulseaudio.PulseAudioTargetPort ; \
	$(JAVAH) -d $(ICEDTEA_SOUND_NATIVE_BUILDDIR) -classpath $(ICEDTEA_SOUND_CLASS_DIR) \
	  -J-Xbootclasspath/p:$(ICEDTEA_SOUND_CLASS_DIR) org.classpath.icedtea.pulseaudio.ContextEvent
	mkdir -p stamps
	touch $@

$(ICEDTEA_SOUND_NATIVE_BUILDDIR)/%.o: $(ICEDTEA_SOUND_NATIVE_SRCDIR)/%.c stamps/icedtea-sound-headers.stamp
	$(CC) $(IT_CFLAGS) -fPIC -I$(SYSTEM_JDK_DIR)/include/linux -I$(SYSTEM_JDK_DIR)/include \
	 -I$(ICEDTEA_SOUND_NATIVE_BUILDDIR) -o $@ -c $<

$(ICEDTEA_SOUND_NATIVE_BUILDDIR)/$(TARGET_NAME): $(ICEDTEA_SOUND_NATIVE_OBJECTS)
	$(CC) $(LDFLAGS) -shared -Xlinker -soname=$(LIBRARY_NAME) \
	  $(ICEDTEA_SOUND_NATIVE_OBJECTS) $(LIBPULSE_LIBS) -o $@

clean-icedtea-sound:
	rm -rf $(ICEDTEA_SOUND_NATIVE_BUILDDIR)
	rm -rf $(ICEDTEA_SOUND_CLASS_DIR)
	rm -f icedtea-sound.jar
	if [ -e $(ICEDTEA_SOUND_BUILDDIR) ]; then \
	  rmdir $(ICEDTEA_SOUND_BUILDDIR) ; \
	fi
	rm -f stamps/icedtea-sound*.stamp

stamps/docs.stamp:
if ENABLE_DOCS
	$(JAVADOC) $(JAVADOC_MEM_OPTS) $(JAVADOC_OPTS) \
	 -d ${abs_top_builddir}/docs \
	 -sourcepath $(ICEDTEA_SOUND_JAVA_SRCDIR):$(ICEDTEA_SOUND_JAVA_GENDIR) \
	 -doctitle 'IcedTea-Sound: API Specification' \
	 -windowtitle 'IcedTea-Sound' \
	 -header '<strong>IcedTea-Sound</strong>' \
	 -linksource \
	 $(ITS_PKGS)
endif
	mkdir -p stamps
	touch stamps/docs.stamp

clean-docs:
	rm -rf ${abs_top_builddir}/docs
	rm -f stamps/docs.stamp
