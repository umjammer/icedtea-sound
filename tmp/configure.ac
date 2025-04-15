dnl configure.ac -- Input for configure script
dnl Copyright (C) 2014 Red Hat, Inc.
dnl
dnl This file is part of IcedTea-Sound.
dnl
dnl IcedTea-Sound is free software; you can redistribute it and/or
dnl modify it under the terms of the GNU General Public License as published by
dnl the Free Software Foundation, version 2.
dnl 
dnl IcedTea-Sound is distributed in the hope that it will be useful,
dnl but WITHOUT ANY WARRANTY; without even the implied warranty of
dnl MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
dnl General Public License for more details.
dnl 
dnl You should have received a copy of the GNU General Public License
dnl along with IcedTea-Sound; see the file COPYING.  If not, write to
dnl the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
dnl 02110-1301 USA.

AC_INIT([icedtea-sound], [1.0.1], [distro-pkg-dev@openjdk.java.net])
AM_INIT_AUTOMAKE([1.9 tar-pax foreign])
AM_MAINTAINER_MODE([enable])

# Older automake doesn't generate these correctly
abs_top_builddir=`pwd -P`
AC_SUBST(abs_top_builddir)
abs_top_srcdir=`dirname $0`
cd $abs_top_srcdir
abs_top_srcdir=`pwd`
cd $abs_top_builddir
AC_SUBST(abs_top_srcdir)

AC_CANONICAL_HOST

AC_PROG_CC
if test "x$CC" = x; then
   AC_MSG_ERROR([No C compiler found.])
fi

IT_SET_ARCH_SETTINGS

IT_FIND_TOOLS([FIND], [gfind find])
IT_FIND_TOOL([ZIP], [zip])

IT_CHECK_JAVA_AND_JAVAC_WORK
IT_FIND_JAVAH
IT_FIND_JAR
IT_FIND_JAVADOC
AC_CONFIG_FILES([javac], [chmod +x javac])

IT_CHECK_ENABLE_WARNINGS

dnl Check for pulseaudio libraries.
PKG_CHECK_MODULES(LIBPULSE,[libpulse >= 0.9.11],[LIBPULSE_FOUND=yes]
  ,[LIBPULSE_FOUND=no])
if test "x${LIBPULSE_FOUND}" = xno
then
  AC_MSG_ERROR([Could not find pulseaudio>=0.9.11 libraries - \
  Try installing pulseaudio-libs-devel>=0.9.11.])
fi
AC_SUBST(LIBPULSE_CFLAGS)
AC_SUBST(LIBPULSE_LIBS)

AC_MSG_CHECKING([whether to build documentation])
AC_ARG_ENABLE([docs],
	      [AS_HELP_STRING([--disable-docs],
	      		      [Disable generation of documentation])],
	      [ENABLE_DOCS="${enableval}"], [ENABLE_DOCS='yes'])
AM_CONDITIONAL([ENABLE_DOCS], [test x$ENABLE_DOCS = xyes])
AC_MSG_RESULT(${ENABLE_DOCS})

# Arguments passed to configure.
AC_SUBST(CONFIGURE_ARGS)
CONFIGURE_ARGS="$ac_configure_args"

AC_CONFIG_FILES([Makefile
	src/java/org/classpath/icedtea/Config.java
])
AC_OUTPUT
