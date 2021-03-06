package test

import org.junit.Test
import slatekit.apis.ApiReg
import slatekit.apis.containers.ApiContainerCLI
import slatekit.integration.apis.AppApi
import slatekit.integration.apis.VersionApi
import slatekit.sampleapp.core.apis.SampleEntityApi
import slatekit.sampleapp.core.apis.SampleExtendedApi
import slatekit.sampleapp.core.apis.SamplePOKOApi


class Api_Setup_Tests : ApiTestsBase() {


    @Test fun can_setup_instance_as_new() {
        val apis = ApiContainerCLI(ctx, apis = listOf(ApiReg(SamplePOKOApi::class)), auth = null)
        val result = apis.getApi("", "SamplePOKO", "getTime" )
        assert(result.success && result.value!!.instance is SamplePOKOApi)
        assert((result.value!!.instance as SamplePOKOApi).count == 0)
    }


    @Test fun can_setup_instance_as_new_with_context() {
        val apis = ApiContainerCLI(ctx, apis = listOf(ApiReg(SampleEntityApi::class)), auth = null)
        val result = apis.getApi("", "SampleEntity", "patch" )
        assert(result.success && result.value!!.instance is SampleEntityApi)
    }


    @Test fun can_setup_instance_as_singleton() {
        val inst = SamplePOKOApi()
        inst.count = 1001
        val apis = ApiContainerCLI(ctx, apis = listOf(ApiReg(inst)), auth = null)
        val result = apis.getApi("", "SamplePOKO", "getTime" )
        assert(result.success && result.value!!.instance is SamplePOKOApi)
        assert((result.value!!.instance as SamplePOKOApi).count == 1001)
    }


    @Test fun can_setup_instance_with_declared_members_only() {
        val apis = ApiContainerCLI(ctx, apis = listOf(ApiReg(SamplePOKOApi::class)), auth = null)
        assert( apis.getApi(""   , "SamplePOKO", "getTime"    ).success)
        assert( apis.getApi(""   , "SamplePOKO", "getCounter" ).success)
        assert( apis.getApi(""   , "SamplePOKO", "hello"      ).success)
        assert( apis.getApi(""   , "SamplePOKO", "request"    ).success)
        assert( apis.getApi(""   , "SamplePOKO", "response"   ).success)
        assert(!apis.getApi(""   , "SamplePOKO", "getEmail"   ).success)
        assert(!apis.getApi(""   , "SamplePOKO", "getSsn"     ).success)
    }


    @Test fun can_setup_instance_with_inheritance() {
        val apis = ApiContainerCLI(ctx, apis = listOf(ApiReg(SampleExtendedApi::class, declaredOnly = false)), auth = null)
        assert( apis.getApi(""   , "SampleExtended", "getSeconds" ).success)
        assert( apis.getApi(""   , "SampleExtended", "getTime"    ).success)
        assert( apis.getApi(""   , "SampleExtended", "getCounter" ).success)
        assert( apis.getApi(""   , "SampleExtended", "hello"      ).success)
        assert( apis.getApi(""   , "SampleExtended", "request"    ).success)
        assert( apis.getApi(""   , "SampleExtended", "response"   ).success)
        assert(!apis.getApi(""   , "SampleExtended", "getEmail"   ).success)
        assert(!apis.getApi(""   , "SampleExtended", "getSsn"     ).success)
    }


    @Test fun can_register_after_initial_setup() {
        val apis = ApiContainerCLI(ctx, apis = listOf(ApiReg(SamplePOKOApi::class)), auth = null)
        apis.register(ApiReg(AppApi(ctx)))
        apis.register(ApiReg(VersionApi(ctx)))

        assert(apis.getApi(""   , "SamplePOKO", "getTime"  ).success)
        assert(apis.getApi("sys", "app"       , "host"     ).success)
        assert(apis.getApi("sys", "version"   , "java"     ).success)
    }


    @Test fun can_check_action_does_NOT_exist() {
        val apis = ApiContainerCLI(ctx, apis = listOf(ApiReg(SamplePOKOApi::class)), auth = null)
        apis.register(ApiReg(AppApi(ctx)))
        apis.register(ApiReg(VersionApi(ctx)))

        assert(!apis.contains("SamplePOKO.fakeMethod"))
        assert(!apis.contains("sys.app.host2"))
    }


    @Test fun can_check_action_exists() {
        val apis = ApiContainerCLI(ctx, apis = listOf(ApiReg(SamplePOKOApi::class)), auth = null)
        apis.register(ApiReg(AppApi(ctx)))
        apis.register(ApiReg(VersionApi(ctx)))

        assert(apis.contains("SamplePOKO.getCounter"))
        assert(apis.contains("sys.app.host"))
    }


    @Test fun can_call_action_without_area() {
        val apis = ApiContainerCLI(ctx, apis = listOf(ApiReg(SamplePOKOApi::class)), auth = null)
        val result = apis.call("", "SamplePOKO", "getCounter", "", mapOf(), mapOf())
        assert(result.success)
        assert(result.value == 1)
    }


    @Test fun can_call_action_in_derived_class() {
        val apis = ApiContainerCLI(ctx, apis = listOf(ApiReg(SampleExtendedApi::class)), auth = null)
        val result = apis.call("", "SampleExtended", "getSeconds", "", mapOf(), mapOf())
        assert(result.success)
        assert(result.value in 0..59)
    }


    @Test fun can_call_action_in_base_class() {
        val apis = ApiContainerCLI(ctx, apis = listOf(ApiReg(SampleExtendedApi::class, declaredOnly = false)), auth = null)
        val result = apis.call("", "SampleExtended", "getCounter", "", mapOf(), mapOf())
        assert(result.success)
        assert(result.value == 1)
    }
}