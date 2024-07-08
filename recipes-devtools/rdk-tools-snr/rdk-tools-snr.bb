DESCRIPTION = "Intel Runtime Development Tools"
HOMEPAGE = "https://www.intel.com"

LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-or-later;md5=fed54355545ffd980b814dab4a3b312c"

SRC_URI = "${RDK_TOOLS_SOURCE} \
           file://rdk-tools.sh \
           file://0001-rdk-snr-remove-THIS_MODULE-from-class_create.patch \
           file://0002-rdk-snr-remove-redundant-pci_enable_pcie_error_repor.patch \
           file://0003-rdk-snr-remove-adf_enable_aer-and-adf_disable_aer.patch \
           file://0004-rdk-snr-change-do_exit-to-kthread_complete_and_exit.patch \
           file://0005-rdk-snr-drop-NAPI_POLL_WEIGHT.patch \
           file://0006-rdk-snr-fix-const-netdev-dev_addr-issue.patch \
           file://0007-rdk-snr-replace-obsolete-interface-u64_stats_fetch_b.patch \
           file://0008-rdk-snr-klm-cpk-ice_txrx.c-include-xdp.h.patch \
           file://0009-rdk-snr-klm-cpk-ice_main.c-add-extack-support-to-ice.patch \
           file://0010-rdk-snr-klm-cpk-ice_fltr.h-use-int-for-ice_fltr_add_.patch \
           file://0011-rdk-snr-klm-cpk-ice_ethtool.c-add-kernel_ringparam-a.patch \
           file://0012-rdk-snr-klm-cpk-ice_devlink.c-remove-devlink_reload_.patch \
           file://0013-rdk-snr-klm-cpk-ice_ptp.c-convert-.adjfreq-to-.adjfi.patch \
           file://0014-rdk-snr-MODULE_ALIAS_CRYPTO-was-moved-to-algapi.h.patch \
           file://0015-rdk-snr-use-tfm-instead-of-reqsize-for-akcipher_alg-.patch \
           file://0016-rdk-snr-add-default_groups-and-ATTRIBUTE_GROUPS-for-.patch \
           file://0017-rdk-snr-change-DEFINE_SEMAPHORE-to-take-an-argument.patch \
           file://0018-rdk-snr-add-gfp-parameter-to-iommu_map.patch \
           file://0019-rdk-snr-replace-vma-vm_flags-direct-modifications.patch \
           file://0020-rdk-snr-replace-random_ether_addr-with-eth_hw_addr_r.patch \
           file://0021-rdk-snr-remove-not-supported-adk_netd_ethtool_get_ri.patch \
           file://0022-rdk-snr-change-PMD_PAGE_SIZE-to-PMD_SIZE.patch \
           file://0023-rdk-snr-netif_rx_ni-skb-netif_rx-skb.patch \
           file://0024-rdk-snr-change-to-for-KBUILD_EXTRA_SYMBOLS.patch \
           file://0025-rdk-snr-use-ether_address_set-instead-of-memcpy.patch \
          "

# RDK user space and kernel module source packages
# Define this if the files exist.  Usually done in template feature/snowridge-rdk.
# For example: RDK_TOOLS_SOURCE ?= "file://rdk_user_src_snr.tgz file://rdk_klm_src_snr.tgz"
RDK_TOOLS_SOURCE ??= ""

COMPATIBLE_MACHINE = "null"

inherit module meson pkgconfig autotools

# Currently supported version
RDK_TOOLS_VERSION ?= "231003"

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
	install -m 0644  ${D}${RDK_INSTALL_DIR}/drivers/*.ko ${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION}/kernel/drivers/staging/intel-rdk/

	install -d ${D}${nonarch_base_libdir}/firmware
	install -D -m 0644 ${D}${RDK_INSTALL_DIR}/drivers/qat_c4xxx.bin  ${D}${nonarch_base_libdir}/firmware/qat_c4xxx.bin
	install -D -m 0644 ${D}${RDK_INSTALL_DIR}/drivers/qat_c4xxx_mmp.bin ${D}${nonarch_base_libdir}/firmware/qat_c4xxx_mmp.bin

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
KERNEL_MODULE_PROBECONF += "adk_netd dlb2 i3c_rdk ice_sw \
                            ice_sw_ae ies intel_vsec \
                            ipsec_inline oobmsm_rdk \
                            intel_qat qat_300xx qat_c3xxx qat_c4xxx \
                           "

# The following kernel drivers will not be autoloaded during kernel boot.
# User can use "modprobe" to load drivers which they will use.
module_conf_adk_netd = "blacklist adk_netd"
module_conf_dlb2 = "blacklist dlb2"
module_conf_i3c_rdk = "blacklist i3c_rdk"
module_conf_ice_sw = "blacklist ice_sw"
module_conf_ice_sw_ae = "blacklist ice_sw_ae"
module_conf_ies = "blacklist ies"
module_conf_intel_vsec = "blacklist intel_vsec"
module_conf_ipsec_inline = "blacklist ipsec_inline"
module_conf_oobmsm_rdk = "blacklist oobmsm_rdk"
module_conf_intel_qat = "blacklist intel_qat"
module_conf_qat_300xx = "blacklist qat_300xx"
module_conf_qat_c3xxx = "blacklist qat_c3xxx"
module_conf_qat_c4xxx = "blacklist qat_c4xxx"

INSANE_SKIP:${PN} = "already-stripped ldflags"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
