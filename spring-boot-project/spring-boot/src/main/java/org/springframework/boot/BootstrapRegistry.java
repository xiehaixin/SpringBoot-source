/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot;

import java.util.function.Supplier;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * A simple object registry that is available during startup and {@link Environment}
 * post-processing up to the point that the {@link ApplicationContext} is prepared.
 *
 * 一个简单的对象注册表，在启动和环境后处理期间可用，直到ApplicationContext准备好。
 *
 * <p>
 * Can be used to register instances that may be expensive to create, or need to be shared
 * before the {@link ApplicationContext} is available.
 *
 * 可用于注册实例，这些实例的创建成本可能很高，或者在ApplicationContext可用之前需要共享。
 *
 * <p>
 * The registry uses {@link Class} as a key, meaning that only a single instance of a
 * given type can be stored.
 *
 * 注册表使用Class作为键，这意味着只能存储给定类型的单个实例。
 *
 * <p>
 * The {@link #addCloseListener(ApplicationListener)} method can be used to add a listener
 * that can perform actions when {@link BootstrapContext} has been closed and the
 * {@link ApplicationContext} is fully prepared. For example, an instance may choose to
 * register itself as a regular Spring bean so that it is available for the application to
 * use.
 *
 * #addCloseListener(ApplicationListener)方法可用于添加一个侦听器，
 * 当BootstrapContext已经关闭且ApplicationContext已经完全准备好时，该侦听器可以执行操作。
 * 例如，一个实例可以选择将自己注册为常规Spring bean，以便应用程序可以使用它。
 *
 * @author Phillip Webb
 * @since 2.4.0
 * @see BootstrapContext
 * @see ConfigurableBootstrapContext
 */
public interface BootstrapRegistry {

	/**
	 * Register a specific type with the registry. If the specified type has already been
	 * registered and has not been obtained as a {@link Scope#SINGLETON singleton}, it
	 * will be replaced.
	 *
	 * 向注册中心注册特定类型。 如果指定的类型已经注册，并且没有作为Scope#SINGLETON SINGLETON获得，它将被替换。
	 *
	 * @param <T> the instance type
	 * @param type the instance type
	 * @param instanceSupplier the instance supplier
	 */
	<T> void register(Class<T> type, InstanceSupplier<T> instanceSupplier);

	/**
	 * Register a specific type with the registry if one is not already present.
	 *
	 * 如果不存在特定类型，则向注册表注册特定类型。
	 *
	 * @param <T> the instance type
	 * @param type the instance type
	 * @param instanceSupplier the instance supplier
	 */
	<T> void registerIfAbsent(Class<T> type, InstanceSupplier<T> instanceSupplier);

	/**
	 * Return if a registration exists for the given type.
	 *
	 * 如果给定类型存在注册则返回。
	 *
	 * @param <T> the instance type
	 * @param type the instance type
	 * @return {@code true} if the type has already been registered
	 */
	<T> boolean isRegistered(Class<T> type);

	/**
	 * Return any existing {@link InstanceSupplier} for the given type.
	 *
	 * 返回给定类型的任何现有instancesprovider。
	 *
	 * @param <T> the instance type
	 * @param type the instance type
	 * @return the registered {@link InstanceSupplier} or {@code null}
	 */
	<T> InstanceSupplier<T> getRegisteredInstanceSupplier(Class<T> type);

	/**
	 * Add an {@link ApplicationListener} that will be called with a
	 * {@link BootstrapContextClosedEvent} when the {@link BootstrapContext} is closed and
	 * the {@link ApplicationContext} has been prepared.
	 *
	 *
	 * 添加一个ApplicationListener，当BootstrapContext关闭并且ApplicationContext已经准备好时，
	 * 它将被BootstrapContextClosedEvent调用。
	 *
	 * @param listener the listener to add
	 */
	void addCloseListener(ApplicationListener<BootstrapContextClosedEvent> listener);

	/**
	 * Supplier used to provide the actual instance when needed.
	 *
	 * 供应商用于在需要时提供实际实例。
	 *
	 * @param <T> the instance type
	 * @see Scope
	 */
	@FunctionalInterface
	interface InstanceSupplier<T> {

		/**
		 * Factory method used to create the instance when needed.
		 *
		 * 工厂方法，用于在需要时创建实例。
		 *
		 * @param context the {@link BootstrapContext} which may be used to obtain other
		 * bootstrap instances.
		 *
		 * BootstrapContext，可以用来获取其他的bootstrap实例。
		 *
		 * @return the instance
		 */
		T get(BootstrapContext context);

		/**
		 * Return the scope of the supplied instance.
		 *
		 * 返回提供的实例的作用域。
		 *
		 * @return the scope
		 * @since 2.4.2
		 */
		default Scope getScope() {
			return Scope.SINGLETON;
		}

		/**
		 * Return a new {@link InstanceSupplier} with an updated {@link Scope}.
		 *
		 * 返回一个新的instancesprovider，其作用域已更新。
		 *
		 * @param scope the new scope
		 * @return a new {@link InstanceSupplier} instance with the new scope
		 * @since 2.4.2
		 */
		default InstanceSupplier<T> withScope(Scope scope) {
			Assert.notNull(scope, "Scope must not be null");
			InstanceSupplier<T> parent = this;
			return new InstanceSupplier<T>() {

				@Override
				public T get(BootstrapContext context) {
					return parent.get(context);
				}

				@Override
				public Scope getScope() {
					return scope;
				}

			};
		}

		/**
		 * Factory method that can be used to create an {@link InstanceSupplier} for a
		 * given instance.
		 *
		 * 工厂方法，可用于为给定实例创建InstanceSupplier。
		 *
		 * @param <T> the instance type
		 * @param instance the instance
		 * @return a new {@link InstanceSupplier}
		 */
		static <T> InstanceSupplier<T> of(T instance) {
			return (registry) -> instance;
		}

		/**
		 * Factory method that can be used to create an {@link InstanceSupplier} from a
		 * {@link Supplier}.
		 *
		 * 工厂方法，可用于从Supplier创建一个instancesprovider。
		 *
		 * @param <T> the instance type
		 * @param supplier the supplier that will provide the instance
		 * @return a new {@link InstanceSupplier}
		 */
		static <T> InstanceSupplier<T> from(Supplier<T> supplier) {
			return (registry) -> (supplier != null) ? supplier.get() : null;
		}

	}

	/**
	 * The scope of a instance.
	 *
	 * 实例的作用域。
	 *
	 * @since 2.4.2
	 */
	enum Scope {

		/**
		 * A singleton instance. The {@link InstanceSupplier} will be called only once and
		 * the same instance will be returned each time.
		 *
		 * 一个单例实例。 instancesprovider只会被调用一次，并且每次都会返回相同的实例。
		 *
		 */
		SINGLETON,

		/**
		 * A prototype instance. The {@link InstanceSupplier} will be called whenever an
		 * instance is needed.
		 *
		 * 一个原型实例。 只要需要实例，就会调用instancesprovider。
		 *
		 */
		PROTOTYPE

	}

}
