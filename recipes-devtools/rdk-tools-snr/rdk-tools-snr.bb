DESCRIPTION = "Intel Runtime Development Tools"
HOMEPAGE = "https://www.intel.com"

LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-or-later;md5=fed54355545ffd980b814dab4a3b312c"

SRC_URI = "${RDK_TOOLS_SOURCE} \
           file://rdk-tools.sh \
          "

# RDK user space and kernel module source packages
# Define this if the files exist.  Usually done in template feature/snowridge-rdk.
# For example: RDK_TOOLS_SOURCE ?= "file://rdk_user_src_snr.tgz file://rdk_klm_src_snr.tgz"
RDK_TOOLS_SOURCE ??= ""

COMPATIBLE_MACHINE = "null"

inherit module meson pkgconfig autotools

# Currently supported version
RDK_TOOLS_VERSION ?= "2410"

S = "${WORKDIR}/rdk"
PV = "${RDK_TOOLS_VERSION}"

DEPENDS = "virtual/kernel libnl libpcap openssl rsync-native thrift meson-native lttng-ust lttng-tools"
RDEPENDS_${PN} += "lttng-ust lttng-tools"

#RDK Tools installed path
RDK_INSTALL_DIR ?= "/opt/intel/rdk-tools"

export KSRC = "${STAGING_KERNEL_BUILDDIR}"
export SDKTARGETSYSROOT = "${STAGING_DIR_TARGET}"
export OECORE_NATIVE_SYSROOT = "${STAGING_DIR_NATIVE}"
export KCFLAGS = "-fmacro-prefix-map=${S}= -fdebug-prefix-map=${S}= "
export BMSM_MODE = "1"

TARGET_CC_ARCH += "${LDFLAGS}"
EXTRA_OEMAKE = "V=1"

do_configure[noexec] = "1"

do_compile () {

	# RDK meson.build is not compatible with meson-wrapper changed in oe-core
	# commit 87d4f6d176f2 ("meson: provide relocation script and native/cross wrappers also for meson-native").
	# So remove meson-wrapper in RDK build and use the "real" meson.
	rm -f ${STAGING_DIR_NATIVE}/usr/bin/meson
	ln -s ${STAGING_DIR_NATIVE}/usr/bin/meson.real ${STAGING_DIR_NATIVE}/usr/bin/meson

	cd ${S}
	oe_runmake modules
	oe_runmake cpk-ae-lib
	oe_runmake netd-lib
	oe_runmake qat_lib
	oe_runmake ies_api_install
	oe_runmake -j1 nura
	oe_runmake cli
}

do_install () {
	oe_runmake -C ${S} install

	install -d ${D}${RDK_INSTALL_DIR}
	cp -r ${S}/install/*  ${D}${RDK_INSTALL_DIR}
	if [ -e ${D}${RDK_INSTALL_DIR}/include/Makefile ]; then
		rm -f ${D}${RDK_INSTALL_DIR}/include/Makefile
	fi

	install -d ${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION}/kernel/drivers/staging/intel-rdk
	install -m 0644  ${D}${RDK_INSTALL_DIR}/drivers/*.ko ${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION}/kernel/drivers/staging/intel-rdk

	install -d ${D}${nonarch_base_libdir}/firmware
	install -D -m 0644 ${D}${RDK_INSTALL_DIR}/drivers/qat_c4xxx.bin  ${D}${nonarch_base_libdir}/firmware/qat_c4xxx.bin
	install -D -m 0644 ${D}${RDK_INSTALL_DIR}/drivers/qat_c4xxx_mmp.bin ${D}${nonarch_base_libdir}/firmware/qat_c4xxx_mmp.bin

	# All drivers and firmware have been installed to proper locatoin. Remove the redundant copies.
	rm -rf ${D}${RDK_INSTALL_DIR}/drivers
	rm -rf ${D}${RDK_INSTALL_DIR}/lib/firmware

	rm -rf ${D}/etc

	install -d ${D}${sysconfdir}/profile.d/
	install -m 0755 ${WORKDIR}/rdk-tools.sh ${D}${sysconfdir}/profile.d/
}

SYSROOT_DIRS += "/opt"

FILES:${PN}-staticdev = "${RDK_INSTALL_DIR}/lib/libies*.a"
FILES:${PN} += "${RDK_INSTALL_DIR} \
                /usr/lib/firmware \
                ${sysconfdir}/profile.d/rdk-tools.sh \
                ${RDK_INSTALL_DIR}/lib/libies*.so \
               "
KERNEL_MODULE_PROBECONF += "adk_netd dlb2 ice_sw \
                            ice_sw_ae ies intel_vsec \
                            ipsec_inline oobmsm_rdk \
                            intel_qat qat_c4xxx \
                           "

# The following kernel drivers will not be autoloaded during kernel boot.
# User can use "modprobe" to load drivers which they will use.
module_conf_adk_netd = "blacklist adk_netd"
module_conf_dlb2 = "blacklist dlb2"
module_conf_ice_sw = "blacklist ice_sw"
module_conf_ice_sw_ae = "blacklist ice_sw_ae"
module_conf_ies = "blacklist ies"
module_conf_intel_vsec = "blacklist intel_vsec"
module_conf_ipsec_inline = "blacklist ipsec_inline"
module_conf_oobmsm_rdk = "blacklist oobmsm_rdk"
module_conf_intel_qat = "blacklist intel_qat"
module_conf_qat_c4xxx = "blacklist qat_c4xxx"

INSANE_SKIP:${PN} = "already-stripped ldflags"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INSANE_SKIP:${PN} += "buildpaths"
