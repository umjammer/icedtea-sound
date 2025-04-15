dnl acinclude.m4 -- Additional macros for configure.ac
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

AC_DEFUN([IT_SET_ARCH_SETTINGS],
[
  case "${host_cpu}" in
    x86_64)
      BUILD_ARCH_DIR=amd64
      INSTALL_ARCH_DIR=amd64
      JRE_ARCH_DIR=amd64
      ARCHFLAG="-m64"
      ;;
    i?86)
      BUILD_ARCH_DIR=i586
      INSTALL_ARCH_DIR=i386
      JRE_ARCH_DIR=i386
      ARCH_PREFIX=${LINUX32}
      ARCHFLAG="-m32"
      ;;
    alpha*)
      BUILD_ARCH_DIR=alpha
      INSTALL_ARCH_DIR=alpha
      JRE_ARCH_DIR=alpha
      ;;
    arm*)
      BUILD_ARCH_DIR=arm
      INSTALL_ARCH_DIR=arm
      JRE_ARCH_DIR=arm
      ARCHFLAG="-D_LITTLE_ENDIAN"
      ;;
    arm64|aarch64)
      BUILD_ARCH_DIR=aarch64
      INSTALL_ARCH_DIR=aarch64
      JRE_ARCH_DIR=aarch64
      ARCHFLAG="-D_LITTLE_ENDIAN"
      ;;
    mips)
      BUILD_ARCH_DIR=mips
      INSTALL_ARCH_DIR=mips
      JRE_ARCH_DIR=mips
       ;;
    mipsel)
      BUILD_ARCH_DIR=mipsel
      INSTALL_ARCH_DIR=mipsel
      JRE_ARCH_DIR=mipsel
       ;;
    powerpc)
      BUILD_ARCH_DIR=ppc
      INSTALL_ARCH_DIR=ppc
      JRE_ARCH_DIR=ppc
      ARCH_PREFIX=${LINUX32}
      ARCHFLAG="-m32"
      ;;
    powerpc64)
      BUILD_ARCH_DIR=ppc64
      INSTALL_ARCH_DIR=ppc64
      JRE_ARCH_DIR=ppc64
      ARCHFLAG="-m64"
       ;;
    powerpc64le)
      BUILD_ARCH_DIR=ppc64
      INSTALL_ARCH_DIR=ppc64
      JRE_ARCH_DIR=ppc64
      ARCHFLAG="-m64"
       ;;
    sparc)
      BUILD_ARCH_DIR=sparc
      INSTALL_ARCH_DIR=sparc
      JRE_ARCH_DIR=sparc
      CROSS_TARGET_ARCH=sparc
      ARCH_PREFIX=${LINUX32}
      ARCHFLAG="-m32"
       ;;
    sparc64)
      BUILD_ARCH_DIR=sparcv9
      INSTALL_ARCH_DIR=sparcv9
      JRE_ARCH_DIR=sparc64
      ARCHFLAG="-m64"
       ;;
    s390)
      BUILD_ARCH_DIR=s390
      INSTALL_ARCH_DIR=s390
      JRE_ARCH_DIR=s390
      ARCH_PREFIX=${LINUX32}
      ARCHFLAG="-m31"
       ;;
    s390x)
      BUILD_ARCH_DIR=s390x
      INSTALL_ARCH_DIR=s390x
      JRE_ARCH_DIR=s390x
      CROSS_TARGET_ARCH=s390x
      ARCHFLAG="-m64"
      ;;
    sh*)
      BUILD_ARCH_DIR=sh
      INSTALL_ARCH_DIR=sh
      JRE_ARCH_DIR=sh
      CROSS_TARGET_ARCH=sh
      ;;
    *)
      BUILD_ARCH_DIR=`uname -m`
      INSTALL_ARCH_DIR=$BUILD_ARCH_DIR
      JRE_ARCH_DIR=$INSTALL_ARCH_DIR
      ;;
  esac
  AC_SUBST(BUILD_ARCH_DIR)
  AC_SUBST(INSTALL_ARCH_DIR)
  AC_SUBST(JRE_ARCH_DIR)
  AC_SUBST(ARCH_PREFIX)
  AC_SUBST(ARCHFLAG)
])

AC_DEFUN([IT_FIND_COMPILER],
[
  AC_REQUIRE([IT_FIND_JAVA])
  AC_REQUIRE([IT_FIND_ECJ_JAR])

  IT_FIND_JAVAC
  IT_FIND_ECJ
  IT_USING_ECJ
  
  if test "x${ECJ_JAR}" = "xno"; then
    if test "x${JAVAC}" = "x"; then
      AC_MSG_ERROR("No compiler or ecj JAR file was found.")
    fi
  fi

  AC_SUBST(ECJ)
  AC_SUBST(JAVAC)
])

AC_DEFUN_ONCE([IT_FIND_ECJ],
[
  ECJ_DEFAULT=/usr/bin/ecj
  AC_MSG_CHECKING([if an ecj binary was specified])
  AC_ARG_WITH([ecj],
	      [AS_HELP_STRING(--with-ecj,bytecode compilation with ecj)],
  [
    if test "x${withval}" = "xyes"; then
      ECJ=no
    else
      ECJ="${withval}"
    fi
  ],
  [ 
    ECJ=no
  ])
  AC_MSG_RESULT(${ECJ})
  if test "x${ECJ}" = "xno"; then
    ECJ=${ECJ_DEFAULT}
  fi
  AC_MSG_CHECKING([if $ECJ is a valid executable file])
  if test -x "${ECJ}" && test -f "${ECJ}"; then
    AC_MSG_RESULT([yes])
  else
    ECJ=""
    AC_PATH_PROG(ECJ, "ecj")
    if test -z "${ECJ}"; then
      AC_PATH_PROG(ECJ, "ecj-3.1")
    fi
    if test -z "${ECJ}"; then
      AC_PATH_PROG(ECJ, "ecj-3.2")
    fi
    if test -z "${ECJ}"; then
      AC_PATH_PROG(ECJ, "ecj-3.3")
    fi
    if test -z "${ECJ}"; then
      AC_PATH_PROG(ECJ, "ecj-3.4")
    fi
  fi
])

AC_DEFUN_ONCE([IT_FIND_JAVAC],
[
  AC_REQUIRE([IT_CHECK_FOR_JDK])
  JAVAC_DEFAULT=${SYSTEM_JDK_DIR}/bin/javac
  AC_MSG_CHECKING([if a javac binary was specified])
  AC_ARG_WITH([javac],
	      [AS_HELP_STRING([--with-javac[[=PATH]]],the path to a javac binary)],
  [
    if test "x${withval}" = "xyes"; then
      JAVAC=no
    else
      JAVAC="${withval}"
    fi
  ],
  [
    JAVAC=no
  ])
  AC_MSG_RESULT(${JAVAC})
  if test "x${JAVAC}" = "xno"; then
    JAVAC=${JAVAC_DEFAULT}
  fi
  AC_MSG_CHECKING([if $JAVAC is a valid executable file])
  if test -x "${JAVAC}" && test -f "${JAVAC}"; then
    AC_MSG_RESULT([yes])
  else
    AC_MSG_RESULT([no])
    JAVAC=""
    AC_PATH_PROG(JAVAC, "javac")
  fi
  AC_SUBST(JAVAC)
  ])
])

AC_DEFUN_ONCE([IT_FIND_JAVA],
[
  AC_REQUIRE([IT_CHECK_FOR_JDK])
  JAVA_DEFAULT=${SYSTEM_JDK_DIR}/bin/java
  AC_MSG_CHECKING([if a java binary was specified])
  AC_ARG_WITH([java],
              [AS_HELP_STRING([--with-java[[=PATH]]],specify location of a 1.5 Java VM)],
  [
    if test "x${withval}" = "xyes"; then
      JAVA=no
    else
      JAVA="${withval}"
    fi
  ],
  [
    JAVA=no
  ])
  AC_MSG_RESULT(${JAVA})
  if test "x${JAVA}" = "xno"; then
    JAVA=${JAVA_DEFAULT}
  fi
  AC_MSG_CHECKING([if $JAVA is a valid executable file])
  if test -x "${JAVA}" && test -f "${JAVA}"; then
    AC_MSG_RESULT([yes])
  else
    AC_MSG_RESULT([no])
    JAVA=""
    AC_PATH_PROG(JAVA, "java")
    if test -z "${JAVA}"; then
      AC_PATH_PROG(JAVA, "gij")
    fi
    if test -z "${JAVA}"; then
      AC_PATH_PROG(JAVA, "cacao")
    fi
    if test -z "${JAVA}"; then
      AC_MSG_ERROR("A 1.5-compatible Java VM is required.")
    fi
  fi
  AC_SUBST(JAVA)
])

AC_DEFUN_ONCE([IT_FIND_ECJ_JAR],
[
  AC_MSG_CHECKING([for an ecj JAR file])
  AC_ARG_WITH([ecj-jar],
              [AS_HELP_STRING([--with-ecj-jar[[=PATH]]],specify location of an ECJ JAR file)],
  [
    if test -f "${withval}"; then
      ECJ_JAR="${withval}"
    fi
  ],
  [
    ECJ_JAR=
  ])
  if test -z "${ECJ_JAR}"; then
    for jar in /usr/share/java/eclipse-ecj.jar \
      /usr/share/java/ecj.jar \
      /usr/share/eclipse-ecj-3.{2,3,4,5}/lib/ecj.jar; do
        if test -e $jar; then
          ECJ_JAR=$jar
	  break
        fi
      done
      if test -z "${ECJ_JAR}"; then
        ECJ_JAR=no
      fi
  fi
  AC_MSG_RESULT(${ECJ_JAR})
  AC_SUBST(ECJ_JAR)
])

AC_DEFUN_ONCE([IT_FIND_JAVAH],
[
  AC_REQUIRE([IT_CHECK_FOR_JDK])
  JAVAH_DEFAULT=${SYSTEM_JDK_DIR}/bin/javah
  AC_MSG_CHECKING([if a javah executable is specified])
  AC_ARG_WITH([javah],
              [AS_HELP_STRING([--with-javah[[=PATH]]],specify location of javah)],
  [
    if test "x${withval}" = "xyes"; then
      JAVAH=no
    else
      JAVAH="${withval}"
    fi
  ],
  [
    JAVAH=no
  ])
  AC_MSG_RESULT(${JAVAH})
  if test "x${JAVAH}" == "xno"; then
    JAVAH=${JAVAH_DEFAULT}
  fi
  AC_MSG_CHECKING([if $JAVAH is a valid executable file])
  if test -x "${JAVAH}" && test -f "${JAVAH}"; then
    AC_MSG_RESULT([yes])
  else
    AC_MSG_RESULT([no])
    JAVAH=""
    AC_PATH_PROG(JAVAH, "javah")
    if test -z "${JAVAH}"; then
      AC_PATH_PROG(JAVAH, "gjavah")
    fi
    if test -z "${JAVAH}"; then
      AC_MSG_ERROR("A Java header generator was not found.")
    fi
  fi
  AC_SUBST(JAVAH)
])

AC_DEFUN_ONCE([IT_FIND_JAR],
[
  AC_REQUIRE([IT_CHECK_FOR_JDK])
  JAR_DEFAULT=${SYSTEM_JDK_DIR}/bin/jar
  AC_MSG_CHECKING([if a jar executable is specified])
  AC_ARG_WITH([jar],
              [AS_HELP_STRING([--with-jar[[=PATH]]],specify location of jar)],
  [
    if test "x${withval}" = "xyes"; then
      JAR=no
    else
      JAR="${withval}"
     fi
  ],
  [
    JAR=no
  ])
  AC_MSG_RESULT(${JAR})
  if test "x${JAR}" == "xno"; then
    JAR=${JAR_DEFAULT}
  fi
  AC_MSG_CHECKING([if $JAR is a valid executable file])
  if test -x "${JAR}" && test -f "${JAR}"; then
    AC_MSG_RESULT([yes])
  else
    AC_MSG_RESULT([no])
    JAR=""
    AC_PATH_PROG(JAR, "jar")
    if test -z "${JAR}"; then
      AC_PATH_PROG(JAR, "gjar")
    fi
    if test -z "${JAR}"; then
      AC_MSG_ERROR("No Java archive tool was found.")
    fi
  fi
  AC_SUBST(JAR)
])

AC_DEFUN([IT_FIND_JAVADOC],
[
  AC_REQUIRE([IT_CHECK_FOR_JDK])
  JAVADOC_DEFAULT=${SYSTEM_JDK_DIR}/bin/javadoc
  AC_MSG_CHECKING([if a javadoc executable is specified])
  AC_ARG_WITH([javadoc],
              [AS_HELP_STRING([--with-javadoc[[=PATH]]],specify location of javadoc)],
  [
    if test "x${withval}" = "xyes"; then
      JAVADOC=no
    else
      JAVADOC="${withval}"
     fi
  ],
  [
    JAVADOC=no
  ])
  AC_MSG_RESULT(${JAVADOC})
  if test "x${JAVADOC}" == "xno"; then
    JAVADOC=${JAVADOC_DEFAULT}
  fi
  AC_MSG_CHECKING([if $JAVADOC is a valid executable file])
  if test -x "${JAVADOC}" && test -f "${JAVADOC}"; then
    AC_MSG_RESULT([yes])
  else
    AC_MSG_RESULT([no])
    JAVADOC=""
    AC_PATH_PROG(JAVADOC, "javadoc")
    if test -z "${JAVADOC}"; then
      AC_PATH_PROG(JAVADOC, "gjdoc")
    fi
    if test -z "${JAVADOC}" && test "x$ENABLE_DOCS" = "xyes"; then
      AC_MSG_ERROR("No Java documentation tool was found.")
    fi
  fi
  AC_MSG_CHECKING([whether javadoc supports -J options])
  CLASS=pkg/Test.java
  mkdir tmp.$$
  cd tmp.$$
  mkdir pkg
  cat << \EOF > $CLASS
[/* [#]line __oline__ "configure" */
package pkg;

public class Test
{
  /**
   * Does stuff.
   *
   * @param args arguments from cli.
   */
  public static void main(String[] args)
  {
    System.out.println("Hello World!");
  }
}
]
EOF
  if $JAVADOC -J-Xmx896m pkg >&AS_MESSAGE_LOG_FD 2>&1; then
    JAVADOC_KNOWS_J_OPTIONS=yes
  else
    JAVADOC_KNOWS_J_OPTIONS=no
  fi
  cd ..
  rm -rf tmp.$$
  AC_MSG_RESULT([${JAVADOC_KNOWS_J_OPTIONS}])
  AC_SUBST(JAVADOC)
  AC_SUBST(JAVADOC_KNOWS_J_OPTIONS)
  AM_CONDITIONAL([JAVADOC_SUPPORTS_J_OPTIONS], test x"${JAVADOC_KNOWS_J_OPTIONS}" = "xyes")
])

AC_DEFUN([IT_FIND_TOOL],
[AC_PATH_TOOL([$1],[$2])
 if test x"$$1" = x ; then
   AC_MSG_ERROR([The following program was not found on the PATH: $2])
 fi
 AC_SUBST([$1])
])

AC_DEFUN([IT_FIND_TOOLS],
[AC_PATH_PROGS([$1],[$2])
 if test x"$$1" = x ; then
   AC_MSG_ERROR([None of the following programs could be found on the PATH: $2])
 fi
 AC_SUBST([$1])
])

AC_DEFUN_ONCE([IT_CHECK_FOR_JDK],
[
  AC_MSG_CHECKING([for a JDK home directory])
  AC_ARG_WITH([jdk-home],
	      [AS_HELP_STRING([--with-jdk-home[[=PATH]]],
                              [jdk home directory (default is first predefined JDK found)])],
              [
                if test "x${withval}" = xyes
                then
                  SYSTEM_JDK_DIR=
                elif test "x${withval}" = xno
                then
	          SYSTEM_JDK_DIR=
	        else
                  SYSTEM_JDK_DIR=${withval}
                fi
              ],
              [
	        SYSTEM_JDK_DIR=
              ])
  if test -z "${SYSTEM_JDK_DIR}"; then
    AC_MSG_RESULT([not specified])
    if test "x${enable_bootstrap}" = "xyes"; then
      BOOTSTRAP_VMS="/usr/lib/jvm/java-gcj /usr/lib/jvm/gcj-jdk /usr/lib/jvm/cacao";
    fi
    ICEDTEA6_VMS="/usr/lib/jvm/icedtea-6 /usr/lib/jvm/icedtea6 /usr/lib/jvm/java-6-openjdk
    		  /usr/lib/jvm/java-1.6.0-openjdk.x86_64 /usr/lib64/jvm/java-1.6.0-openjdk
		  /usr/lib/jvm/java-1.6.0"
    ICEDTEA7_VMS="/usr/lib/jvm/icedtea-7 /usr/lib/jvm/icedtea7 /usr/lib/jvm/java-1.7.0-openjdk
    		  /usr/lib/jvm/java-1.7.0-openjdk.x86_64 /usr/lib64/jvm/java-1.7.0-openjdk
		  /usr/lib/jvm/java-1.7.0 /usr/lib/jvm/java-7-openjdk"
    for dir in ${BOOTSTRAP_VMS} ${ICEDTEA7_VMS} ${ICEDTEA6_VMS} \
    	       /usr/lib/jvm/java-openjdk /usr/lib/jvm/openjdk /usr/lib/jvm/java-icedtea \
	       /etc/alternatives/java_sdk_openjdk ; do
       AC_MSG_CHECKING([for ${dir}]);
       if test -d $dir; then
         SYSTEM_JDK_DIR=$dir ;
	 AC_MSG_RESULT([found]) ;
	 break ;
       else
         AC_MSG_RESULT([not found]) ;
       fi
    done
  else
    AC_MSG_RESULT(${SYSTEM_JDK_DIR})
  fi
  if ! test -d "${SYSTEM_JDK_DIR}"; then
    AC_MSG_ERROR("A JDK home directory could not be found.")
  fi
  AC_SUBST(SYSTEM_JDK_DIR)
])

AC_DEFUN([IT_USING_ECJ],[
AC_CACHE_CHECK([if we are using ecj as javac], it_cv_ecj, [
if $JAVAC -version 2>&1| grep '^Eclipse' >&AS_MESSAGE_LOG_FD ; then
  it_cv_ecj=yes;
else
  it_cv_ecj=no;
fi
])
USING_ECJ=$it_cv_ecj
AC_SUBST(USING_ECJ)
AC_PROVIDE([$0])dnl
])

dnl check that javac and java work
AC_DEFUN_ONCE([IT_CHECK_JAVA_AND_JAVAC_WORK],[
  AC_REQUIRE([IT_FIND_JAVA])
  AC_REQUIRE([IT_FIND_COMPILER])
  AC_CACHE_CHECK([if the VM and compiler work together], it_cv_jdk_works, [
  CLASS=Test.java
  BYTECODE=$(echo $CLASS|sed 's#\.java##')
  mkdir tmp.$$
  cd tmp.$$
  cat << \EOF > $CLASS
[/* [#]line __oline__ "configure" */

public class Test
{
    public static void main(String[] args)
    {
      System.out.println("Hello World!");
    }
}]
EOF
  if $JAVAC -cp . $JAVACFLAGS -source 5 -target 5 $CLASS >&AS_MESSAGE_LOG_FD 2>&1; then
    if $JAVA -classpath . $BYTECODE >&AS_MESSAGE_LOG_FD 2>&1; then
      it_cv_jdk_works=yes;
    else
      it_cv_jdk_works=no;
      AC_MSG_ERROR([VM failed to run compiled class.])
    fi
  else
    it_cv_jdk_works=no;
    AC_MSG_ERROR([Compiler failed to compile Java code.])
  fi
  rm -f $CLASS *.class
  cd ..
  rmdir tmp.$$
  ])
AC_PROVIDE([$0])dnl
])

AC_DEFUN([IT_CHECK_ENABLE_WARNINGS],
[
  AC_MSG_CHECKING(whether to enable Java compiler warnings)
  AC_ARG_ENABLE([warnings],
	      [AS_HELP_STRING(--enable-warnings,produce warnings from javac/ecj [[default=no]])],
  [
    ENABLE_WARNINGS="${enableval}"
  ],
  [
    ENABLE_WARNINGS=no
  ])

  AC_MSG_RESULT(${ENABLE_WARNINGS})
  AM_CONDITIONAL(ENABLE_WARNINGS, test x"${ENABLE_WARNINGS}" = "xyes")
  AC_SUBST(ENABLE_WARNINGS)
])
