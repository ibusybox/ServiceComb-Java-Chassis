/*
 * Copyright 2017 Huawei Technologies Co., Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.servicecomb.provider.pojo.schema;

import java.util.Collection;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.servicecomb.foundation.common.RegisterManager;
import io.servicecomb.foundation.common.utils.BeanUtils;
import io.servicecomb.provider.pojo.RpcSchema;

@Component
public class PojoProducers implements BeanPostProcessor {
  // key为schemaId
  private RegisterManager<String, PojoProducerMeta> pojoMgr = new RegisterManager<>("pojo service manager");

  public void registerPojoProducer(PojoProducerMeta pojoProducer) {
    pojoMgr.register(pojoProducer.getSchemaId(), pojoProducer);
  }

  public Collection<PojoProducerMeta> getProcucers() {
    return pojoMgr.values();
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    processProvider(beanName, bean);

    return bean;
  }

  protected void processProvider(String beanName, Object bean) {
    // aop后，新的实例的父类可能是原class，也可能只是个proxy，父类不是原class
    // 所以，需要先取出原class，再取标注
    Class<?> beanCls = BeanUtils.getImplClassFromBean(bean);
    RpcSchema rpcSchema = beanCls.getAnnotation(RpcSchema.class);
    if (rpcSchema == null) {
      return;
    }

    String schemaId = rpcSchema.schemaId();
    if (StringUtils.isEmpty(schemaId)) {
      Class<?>[] intfs = beanCls.getInterfaces();
      if (intfs.length == 1) {
        schemaId = intfs[0].getName();
      } else {
        throw new Error("Must be schemaId or implements only one interface");
      }
    }

    PojoProducerMeta pojoProducerMeta = new PojoProducerMeta();
    pojoProducerMeta.setSchemaId(schemaId);
    pojoProducerMeta.setInstance(bean);
    pojoProducerMeta.setInstanceClass(beanCls);

    registerPojoProducer(pojoProducerMeta);
  }
}
