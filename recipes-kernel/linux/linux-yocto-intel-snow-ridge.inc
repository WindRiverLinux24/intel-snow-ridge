KMACHINE:intel-snow-ridge = "intel-x86-64"

COMPATIBLE_MACHINE:intel-snow-ridge = "intel-snow-ridge"

# The in-tree QAT drivers don't support QAT C4xxx Series on Snow Ridge. So remove them.
# When feature/snowridge-rdk is enabled, RDK will provide QAT related drivers.
do_install:append:intel-snow-ridge() {
	rm -rf ${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION}/kernel/drivers/crypto
}
